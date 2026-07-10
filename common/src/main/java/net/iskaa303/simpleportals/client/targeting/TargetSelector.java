package net.iskaa303.simpleportals.client.targeting;

import net.iskaa303.simpleportals.client.keybinds.SimplePortalsKeybinds;
import net.iskaa303.simpleportals.client.render.RenderConstants;
import net.iskaa303.simpleportals.item.PortalStick;
import net.iskaa303.simpleportals.item.PortalStickMode;
import net.iskaa303.simpleportals.item.PointDataStore;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/** Targeting: raycast, grid/point/connection snapping. */
public final class TargetSelector {

    private static Vec3 smoothedRenderPos;
    private static BlockHitResult latestHitResult;
    // Connection snap endpoints
    private static Vec3 snappedConnectionA;
    private static Vec3 snappedConnectionB;
    private static String snappedConnectionUuidA;
    private static String snappedConnectionUuidB;
    // Surface snap
    private static String snappedSurfaceId;
    private static Vec3 snappedSurfaceA;
    private static Vec3 snappedSurfaceB;
    private static List<Vec3> snappedSurfaceVertices;
    // Preview surface vertices (cursor on point with cycle)
    private static List<Vec3> previewSurfaceVerts;
    private TargetSelector() {}

    public static Vec3 getTarget(@Nonnull LocalPlayer player, @Nonnull Camera camera, float partialTicks) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return null;

        Vec3 eyePos = camera.getPosition();
        Vec3 lookVec = player.getViewVector(partialTicks);
        Vec3 reachVec = eyePos.add(lookVec.scale(RenderConstants.REACH));

        latestHitResult = level.clip(
                new ClipContext(eyePos, reachVec, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player)
        );
        BlockHitResult hitResult = latestHitResult;

        Vec3 targetPos = hitResult.getType() != HitResult.Type.MISS
                ? hitResult.getLocation()
                : reachVec;

        snappedConnectionA = null;
        snappedConnectionB = null;
        snappedConnectionUuidA = null;
        snappedConnectionUuidB = null;
        snappedSurfaceId = null;
        snappedSurfaceA = null;
        snappedSurfaceB = null;
        snappedSurfaceVertices = null;
        previewSurfaceVerts = null;

        PortalStickMode mode = PointDataStore.getMode(player);
        List<Vec3> savedPoints = getSavedPoints(player);

        boolean snapToPoint = SimplePortalsKeybinds.isDown(SimplePortalsKeybinds.getSnapPoint());
        boolean snapToGrid = SimplePortalsKeybinds.isDown(SimplePortalsKeybinds.getSnapGrid());

        if (snapToPoint && savedPoints != null && !savedPoints.isEmpty()) {
            Vec3 nearest = getNearestPoint(targetPos, savedPoints);
            if (nearest != null) targetPos = nearest;
        } else if (snapToGrid) {
            if (mode == PortalStickMode.SURFACE && savedPoints != null && !savedPoints.isEmpty()) {
                targetPos = snapToNearestSurface(player, targetPos);
            } else if (mode == PortalStickMode.CONNECTION && savedPoints != null && !savedPoints.isEmpty()) {
                targetPos = snapToNearestConnection(player, targetPos);
            } else {
                targetPos = hitResult.getType() != HitResult.Type.MISS
                        ? snapToSurface(hitResult)
                        : snapToGrid(targetPos);
            }
        }

        // Compute preview surface if cursor is on a point and in surface mode
        if (mode == PortalStickMode.SURFACE) {
            computePreviewSurface(player, targetPos);
        }

        boolean shouldSmooth = snapToGrid || snapToPoint;
        return smoothRenderPos(targetPos, shouldSmooth);
    }

    public static Vec3 getCurrentTarget() {
        return smoothedRenderPos;
    }

    public static BlockHitResult getLastHitResult() {
        return latestHitResult;
    }

    /** If cursor is snapped to a connection, returns the two endpoint positions. */
    @Nullable
    public static Vec3[] getSnappedConnectionEndpoints() {
        if (snappedConnectionA == null || snappedConnectionB == null) return null;
        return new Vec3[]{snappedConnectionA, snappedConnectionB};
    }

    /** If cursor is snapped to a connection, returns the two point UUIDs. */
    @Nullable
    public static String[] getSnappedConnectionUuids() {
        if (snappedConnectionUuidA == null || snappedConnectionUuidB == null) return null;
        return new String[]{snappedConnectionUuidA, snappedConnectionUuidB};
    }

    @Nullable
    public static String getSnappedSurfaceId() {
        return snappedSurfaceId;
    }

    /** If cursor is snapped to a surface edge, returns the two endpoint positions. */
    @Nullable
    public static Vec3[] getSnappedSurfaceEndpoints() {
        if (snappedSurfaceA == null || snappedSurfaceB == null) return null;
        return new Vec3[]{snappedSurfaceA, snappedSurfaceB};
    }

    @Nullable
    public static List<Vec3> getSnappedSurfaceVertices() {
        return snappedSurfaceVertices;
    }

    @Nullable
    public static List<Vec3> getPreviewSurfaceVertices() {
        return previewSurfaceVerts;
    }

    // ─── Helpers ───

    private static boolean isHoldingPortalStick(@Nonnull LocalPlayer player) {
        var stick = net.iskaa303.simpleportals.registry.SimplePortalsItems.PORTAL_STICK.get();
        if (stick == null) return false;
        return player.getMainHandItem().is(stick) || player.getOffhandItem().is(stick);
    }

    private static List<Vec3> getSavedPoints(LocalPlayer player) {
        if (!isHoldingPortalStick(player)) return null;
        return PointDataStore.getPoints(player);
    }

    private static Vec3 snapToGrid(@Nonnull Vec3 pos) {
        return new Vec3(
                Mth.floor(pos.x / RenderConstants.BOX_SIZE) * RenderConstants.BOX_SIZE,
                Mth.floor(pos.y / RenderConstants.BOX_SIZE) * RenderConstants.BOX_SIZE,
                Mth.floor(pos.z / RenderConstants.BOX_SIZE) * RenderConstants.BOX_SIZE
        );
    }

    private static Vec3 snapToSurface(@Nonnull BlockHitResult hitResult) {
        Vec3 pos = hitResult.getLocation();
        Direction face = hitResult.getDirection();
        double half = RenderConstants.BOX_SIZE / 2.0;

        double x = face.getAxis() == Direction.Axis.X
                ? pos.x + (face.getStepX() * half)
                : Math.round(pos.x / RenderConstants.BOX_SIZE) * RenderConstants.BOX_SIZE;
        double y = face.getAxis() == Direction.Axis.Y
                ? pos.y + (face.getStepY() * half)
                : Math.round(pos.y / RenderConstants.BOX_SIZE) * RenderConstants.BOX_SIZE;
        double z = face.getAxis() == Direction.Axis.Z
                ? pos.z + (face.getStepZ() * half)
                : Math.round(pos.z / RenderConstants.BOX_SIZE) * RenderConstants.BOX_SIZE;

        return new Vec3(x, y, z);
    }

    /** Snap to nearest connection segment, store endpoints for cursor. */
    private static Vec3 snapToNearestConnection(@Nonnull LocalPlayer player, @Nonnull Vec3 pos) {
        ListTag points = PointDataStore.getPointList(player);
        Vec3 bestPoint = null;
        Vec3 bestA = null;
        Vec3 bestB = null;
        String bestUuidA = null;
        String bestUuidB = null;
        double bestDistSqr = Double.MAX_VALUE;
        for (int i = 0; i < points.size(); i++) {
            CompoundTag tagA = points.getCompound(i);
            Vec3 a = new Vec3(tagA.getDouble("x"), tagA.getDouble("y"), tagA.getDouble("z"));
            String uuidA = tagA.getString("id");

            ListTag conns = tagA.getList("connections", Tag.TAG_STRING);
            for (int j = 0; j < conns.size(); j++) {
                String connUuid = conns.getString(j);
                if (uuidA.compareTo(connUuid) >= 0) continue; // each pair once

                CompoundTag tagB = findTagByUuid(points, connUuid);
                if (tagB == null) continue;
                Vec3 b = new Vec3(tagB.getDouble("x"), tagB.getDouble("y"), tagB.getDouble("z"));

                Vec3 closest = closestPointOnSegment(pos, a, b);
                double distSqr = pos.distanceToSqr(closest);
                if (distSqr < bestDistSqr) {
                    bestDistSqr = distSqr;
                    bestPoint = closest;
                    bestA = a;
                    bestB = b;
                    bestUuidA = uuidA;
                    bestUuidB = connUuid;
                }
            }
        }

        if (bestPoint != null) {
            snappedConnectionA = bestA;
            snappedConnectionB = bestB;
            snappedConnectionUuidA = bestUuidA;
            snappedConnectionUuidB = bestUuidB;
            return bestPoint;
        }
        return pos;
    }

    /** Snap to nearest surface edge segment, store endpoints for stretched cursor. */
    private static Vec3 snapToNearestSurface(@Nonnull LocalPlayer player, @Nonnull Vec3 pos) {
        ListTag surfaces = PointDataStore.getSurfaces(player);
        Vec3 bestPoint = null;
        String bestSurfaceId = null;
        Vec3 bestA = null;
        Vec3 bestB = null;
        double bestDistSqr = Double.MAX_VALUE;

        for (int s = 0; s < surfaces.size(); s++) {
            CompoundTag surf = surfaces.getCompound(s);
            String surfId = surf.getString("surface_id");
            ListTag ptUuids = surf.getList("points", Tag.TAG_STRING);
            int n = ptUuids.size();
            if (n < 2) continue;
            for (int i = 0; i < n; i++) {
                String uuidA = ptUuids.getString(i);
                String uuidB = ptUuids.getString((i + 1) % n);
                Vec3 a = PointDataStore.getPointPosByUuid(player, uuidA);
                Vec3 b = PointDataStore.getPointPosByUuid(player, uuidB);
                if (a == null || b == null) continue;

                Vec3 closest = closestPointOnSegment(pos, a, b);
                double distSqr = pos.distanceToSqr(closest);
                if (distSqr < bestDistSqr) {
                    bestDistSqr = distSqr;
                    bestPoint = closest;
                    bestSurfaceId = surfId;
                    bestA = a;
                    bestB = b;
                }
            }
        }

        if (bestPoint != null) {
            // Store full polygon vertices for the surface highlight
            List<Vec3> fullVerts = new java.util.ArrayList<>();
            for (int s2 = 0; s2 < surfaces.size(); s2++) {
                CompoundTag check = surfaces.getCompound(s2);
                if (check.getString("surface_id").equals(bestSurfaceId)) {
                    ListTag ptUuids = check.getList("points", Tag.TAG_STRING);
                    for (int k = 0; k < ptUuids.size(); k++) {
                        Vec3 p = PointDataStore.getPointPosByUuid(player, ptUuids.getString(k));
                        if (p != null) fullVerts.add(p);
                    }
                    break;
                }
            }
            snappedSurfaceVertices = fullVerts;
            snappedSurfaceId = bestSurfaceId;
            snappedSurfaceA = bestA;
            snappedSurfaceB = bestB;
            return bestPoint;
        }
        return pos;
    }

    /** Compute preview surface for the point at (or nearest to) the cursor. */
    private static void computePreviewSurface(@Nonnull LocalPlayer player, @Nonnull Vec3 targetPos) {
        String pointUuid = PointDataStore.findPointUuid(player, targetPos);
        if (pointUuid == null) return;
        List<String> cycle = PointDataStore.findSmallestCycleContaining(player, pointUuid);
        if (cycle == null || cycle.size() < 3) return;
        previewSurfaceVerts = PointDataStore.getPositionsByUuids(player, cycle);
    }

    private static CompoundTag findTagByUuid(@Nonnull ListTag list, @Nonnull String uuid) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            if (uuid.equals(tag.getString("id"))) return tag;
        }
        return null;
    }

    private static Vec3 smoothRenderPos(@Nonnull Vec3 targetPos, boolean shouldSmooth) {
        if (!shouldSmooth || smoothedRenderPos == null || smoothedRenderPos.distanceToSqr(targetPos) > 4.0) {
            smoothedRenderPos = targetPos;
            return targetPos;
        }
        smoothedRenderPos = smoothedRenderPos.lerp(targetPos, RenderConstants.SNAP_SMOOTHING);
        return smoothedRenderPos;
    }

    private static Vec3 getNearestPoint(@Nonnull Vec3 target, @Nonnull List<Vec3> points) {
        Vec3 nearest = null;
        double minSqrDist = Double.MAX_VALUE;
        for (Vec3 point : points) {
            double distSqr = point.distanceToSqr(target);
            if (distSqr < minSqrDist) {
                minSqrDist = distSqr;
                nearest = point;
            }
        }
        return nearest;
    }

    /** Closest point on segment AB to P. */
    private static Vec3 closestPointOnSegment(@Nonnull Vec3 p, @Nonnull Vec3 a, @Nonnull Vec3 b) {
        Vec3 ab = b.subtract(a);
        double lenSqr = ab.lengthSqr();
        if (lenSqr < 1e-10) return a;
        double t = p.subtract(a).dot(ab) / lenSqr;
        t = Mth.clamp(t, 0.0, 1.0);
        return a.add(ab.scale(t));
    }
}
