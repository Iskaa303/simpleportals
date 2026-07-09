package net.iskaa303.simpleportals.client.targeting;

import net.iskaa303.simpleportals.client.render.RenderConstants;
import net.iskaa303.simpleportals.item.PointDataStore;
import net.iskaa303.simpleportals.registry.SimplePortalsItems;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
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
    // Connection snap endpoints (Shift + Connection Stick)
    private static Vec3 snappedConnectionA;
    private static Vec3 snappedConnectionB;
    private static String snappedConnectionUuidA;
    private static String snappedConnectionUuidB;

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

        boolean isConnectionStick = isHoldingConnectionStick(player);
        List<Vec3> savedPoints = getSavedPoints(player);

        if (Screen.hasControlDown() && savedPoints != null && !savedPoints.isEmpty()) {
            Vec3 nearest = getNearestPoint(targetPos, savedPoints);
            if (nearest != null) targetPos = nearest;
        } else if (player.isShiftKeyDown()) {
            if (isConnectionStick && savedPoints != null && !savedPoints.isEmpty()) {
                targetPos = snapToNearestConnection(player, targetPos);
            } else {
                targetPos = hitResult.getType() != HitResult.Type.MISS
                        ? snapToSurface(hitResult)
                        : snapToGrid(targetPos);
            }
        }

        boolean shouldSmooth = player.isShiftKeyDown() || Screen.hasControlDown();
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

    // ─── Helpers ───

    private static boolean isHoldingConnectionStick(LocalPlayer player) {
        var connStick = SimplePortalsItems.CONNECTION_STICK.get();
        if (connStick == null) return false;
        return player.getMainHandItem().is(connStick) || player.getOffhandItem().is(connStick);
    }

    private static List<Vec3> getSavedPoints(LocalPlayer player) {
        var pointStick = SimplePortalsItems.POINT_STICK.get();
        var connStick = SimplePortalsItems.CONNECTION_STICK.get();
        boolean hasStick = (pointStick != null && (player.getMainHandItem().is(pointStick) || player.getOffhandItem().is(pointStick)))
                || (connStick != null && (player.getMainHandItem().is(connStick) || player.getOffhandItem().is(connStick)));
        if (!hasStick) return null;
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

    /** Closest point on segment AB to P. */
    private static Vec3 closestPointOnSegment(@Nonnull Vec3 p, @Nonnull Vec3 a, @Nonnull Vec3 b) {
        Vec3 ab = b.subtract(a);
        double lenSqr = ab.lengthSqr();
        if (lenSqr < 1e-10) return a;
        double t = p.subtract(a).dot(ab) / lenSqr;
        t = Mth.clamp(t, 0.0, 1.0);
        return a.add(ab.scale(t));
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
}
