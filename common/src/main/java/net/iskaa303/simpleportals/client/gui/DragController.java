package net.iskaa303.simpleportals.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.iskaa303.simpleportals.client.keybinds.SimplePortalsKeybinds;
import net.iskaa303.simpleportals.client.targeting.TargetSelector;
import net.iskaa303.simpleportals.config.SimplePortalsConfig;
import net.iskaa303.simpleportals.item.PortalStick;
import net.iskaa303.simpleportals.item.PortalStickMode;
import net.iskaa303.simpleportals.item.PointDataStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side drag controller. Manages picking up and moving points, connections, and surfaces.
 * All position overrides are computed as offsets from stored positions; the PointDataStore is
 * only written on endDrag().
 */
public final class DragController {

    public enum DragMode { NONE, POINT, CONNECTION, SURFACE }

    private static DragMode mode = DragMode.NONE;

    // Identifiers for the dragged element
    private static String draggedPointUuid;
    private static String connUuidA, connUuidB;
    private static String draggedSurfaceId;
    private static final List<String> surfacePointUuids = new ArrayList<>();

    // Original stored positions of all affected points (snapshot at drag start)
    private static final Map<String, Vec3> originalPositions = new HashMap<>();

    // Cursor target at drag start — used to compute relative delta
    private static Vec3 dragStartTarget;

    // Edge-detection state
    private static boolean wasAttackDown = false;
    private static boolean wasToggleGridDown = false;

    /** True while a server-side use() call should be suppressed (singleplayer only). */
    public static volatile boolean shouldCancelUse = false;

    // Pick threshold in blocks
    private static final double PICK_THRESHOLD = 0.5;
    private static final double PICK_THRESHOLD_SQR = PICK_THRESHOLD * PICK_THRESHOLD;

    // ─── Color constants exposed to renderers ───

    /** Dragged point cube color. */
    public static final float[] DRAG_POINT_COLOR = {1.0f, 0.6f, 0.0f, 0.9f};
    /** Connection line alpha when one of its endpoints is dragged. */
    public static final float DRAG_CONN_ALPHA = 0.8f;
    /** Dragged surface fill. */
    public static final float[] DRAG_SURFACE_FILL = {1.0f, 0.6f, 0.0f, 0.35f};
    /** Dragged surface outline. */
    public static final float[] DRAG_SURFACE_OUTLINE = {1.0f, 0.8f, 0.2f, 0.7f};

    private DragController() {}

    // ─── Tick ───

    /** Called every client tick from the common tick handler. */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        boolean holding = PortalStick.isHolding(player);

        // --- Grid toggle ---
        boolean toggleDown = SimplePortalsKeybinds.isDown(SimplePortalsKeybinds.getToggleGrid());
        if (holding && toggleDown && !wasToggleGridDown) {
            SimplePortalsConfig.showGrid = !SimplePortalsConfig.showGrid;
            SimplePortalsConfig.save();
        }
        wasToggleGridDown = toggleDown;

        // --- Left-click drag ---
        boolean attackDown = mc.options.keyAttack.isDown();
        if (holding && attackDown && !wasAttackDown) {
            if (mode != DragMode.NONE) {
                endDrag();
            } else {
                tryStartDrag(player);
            }
        }

        // Update drag positions every tick while actively dragging
        if (mode != DragMode.NONE && holding) {
            updateDrag(player);
        } else if (mode != DragMode.NONE && !holding) {
            cancelDrag();
        }

        wasAttackDown = attackDown;
    }

    // ─── Start / End / Cancel ───

    private static void tryStartDrag(LocalPlayer player) {
        PortalStickMode stickMode = PointDataStore.getMode(player);
        Vec3 target = TargetSelector.getCurrentTarget();
        if (target == null) return;

        switch (stickMode) {
            case POINT -> {
                String uuid = PointDataStore.findPointUuid(player, target, PICK_THRESHOLD);
                if (uuid == null) return;
                mode = DragMode.POINT;
                draggedPointUuid = uuid;
                Vec3 pos = PointDataStore.getPointPosByUuid(player, uuid);
                if (pos != null) originalPositions.put(uuid, pos);
                dragStartTarget = target;
                shouldCancelUse = true;
            }
            case CONNECTION -> {
                String[] conn = pickConnection(player, target);
                if (conn == null) return;
                mode = DragMode.CONNECTION;
                connUuidA = conn[0];
                connUuidB = conn[1];
                Vec3 posA = PointDataStore.getPointPosByUuid(player, connUuidA);
                Vec3 posB = PointDataStore.getPointPosByUuid(player, connUuidB);
                if (posA != null) originalPositions.put(connUuidA, posA);
                if (posB != null) originalPositions.put(connUuidB, posB);
                dragStartTarget = target;
                shouldCancelUse = true;
            }
            case SURFACE -> {
                String[] surf = pickSurface(player, target);
                if (surf == null) return;
                mode = DragMode.SURFACE;
                draggedSurfaceId = surf[0];
                surfacePointUuids.clear();
                ListTag surfaces = PointDataStore.getSurfaces(player);
                for (int i = 0; i < surfaces.size(); i++) {
                    CompoundTag s = surfaces.getCompound(i);
                    if (s.getString("surface_id").equals(draggedSurfaceId)) {
                        ListTag ptUuids = s.getList("points", Tag.TAG_STRING);
                        for (int j = 0; j < ptUuids.size(); j++) {
                            String uuid = ptUuids.getString(j);
                            surfacePointUuids.add(uuid);
                            Vec3 p = PointDataStore.getPointPosByUuid(player, uuid);
                            if (p != null) originalPositions.put(uuid, p);
                        }
                        break;
                    }
                }
                dragStartTarget = target;
                shouldCancelUse = true;
            }
        }
    }

    /** Finalise positions — write offsets into PointDataStore. */
    private static void endDrag() {
        if (mode == DragMode.NONE) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player != null) {
            Vec3 target = TargetSelector.getCurrentTarget();
            if (target != null && dragStartTarget != null) {
                Vec3 delta = target.subtract(dragStartTarget);
                for (Map.Entry<String, Vec3> e : originalPositions.entrySet()) {
                    Vec3 newPos = e.getValue().add(delta);
                    // Update the stored point position
                    CompoundTag tag = PointDataStore.getPointByUuid(player, e.getKey());
                    if (tag != null) {
                        tag.putDouble("x", newPos.x);
                        tag.putDouble("y", newPos.y);
                        tag.putDouble("z", newPos.z);
                    }
                }
            }
        }
        clearDrag();
    }

    /** Revert to original stored positions. */
    private static void cancelDrag() {
        // original positions are already in the store, just clear drag
        clearDrag();
    }

    private static void clearDrag() {
        mode = DragMode.NONE;
        draggedPointUuid = null;
        connUuidA = null;
        connUuidB = null;
        draggedSurfaceId = null;
        surfacePointUuids.clear();
        originalPositions.clear();
        dragStartTarget = null;
        shouldCancelUse = false;
    }

    // ─── Update ───

    private static void updateDrag(LocalPlayer player) {
        if (dragStartTarget == null) return;
        Vec3 target = TargetSelector.getCurrentTarget();
        if (target == null) return;
        // No per-frame action needed — renderers read originalPositions and compute
        // offset from dragStartTarget to current target via getDragOffset().
    }

    // ─── Queries for renderers ───

    public static boolean isDragging() {
        return mode != DragMode.NONE;
    }

    public static DragMode getDragMode() {
        return mode;
    }

    /** The offset from the stored position to the live drag position, or zero. */
    public static Vec3 getDragOffset() {
        if (mode == DragMode.NONE || dragStartTarget == null) return Vec3.ZERO;
        Vec3 target = TargetSelector.getCurrentTarget();
        if (target == null) return Vec3.ZERO;
        return target.subtract(dragStartTarget);
    }

    /** True if this point UUID is one of the points being moved by the current drag. */
    public static boolean isPointAffected(String uuid) {
        if (mode == DragMode.NONE || uuid == null) return false;
        return switch (mode) {
            case POINT -> uuid.equals(draggedPointUuid);
            case CONNECTION -> uuid.equals(connUuidA) || uuid.equals(connUuidB);
            case SURFACE -> surfacePointUuids.contains(uuid);
            default -> false;
        };
    }

    /** True if the connection between uuidA and uuidB is being dragged. */
    public static boolean isConnectionAffected(String uuidA, String uuidB) {
        if (mode == DragMode.NONE) return false;
        if (mode == DragMode.CONNECTION) {
            return (uuidA.equals(connUuidA) && uuidB.equals(connUuidB))
                    || (uuidA.equals(connUuidB) && uuidB.equals(connUuidA));
        }
        return isPointAffected(uuidA) || isPointAffected(uuidB);
    }

    /** True if this surface is being dragged. */
    public static boolean isSurfaceAffected(String surfaceId) {
        return mode == DragMode.SURFACE && surfaceId != null && surfaceId.equals(draggedSurfaceId);
    }

    /** Get the display position for a point, applying drag offset if affected. */
    public static Vec3 getDisplayPosition(Vec3 stored, String uuid) {
        if (!isPointAffected(uuid)) return stored;
        Vec3 offset = getDragOffset();
        if (offset == Vec3.ZERO) return stored;
        return stored.add(offset);
    }

    // ─── Picking helpers ───

    /**
     * Find a connection segment near the given position.
     * Returns [uuidA, uuidB] or null.
     */
    @Nullable
    private static String[] pickConnection(LocalPlayer player, Vec3 pos) {
        ListTag points = PointDataStore.getPointList(player);
        double best = Double.MAX_VALUE;
        String bestA = null, bestB = null;
        for (int i = 0; i < points.size(); i++) {
            CompoundTag tagA = points.getCompound(i);
            Vec3 a = new Vec3(tagA.getDouble("x"), tagA.getDouble("y"), tagA.getDouble("z"));
            String uuidA = tagA.getString("id");
            ListTag conns = tagA.getList("connections", Tag.TAG_STRING);
            for (int j = 0; j < conns.size(); j++) {
                String connUuid = conns.getString(j);
                if (uuidA.compareTo(connUuid) >= 0) continue;
                CompoundTag tagB = findTagByUuid(points, connUuid);
                if (tagB == null) continue;
                Vec3 b = new Vec3(tagB.getDouble("x"), tagB.getDouble("y"), tagB.getDouble("z"));
                double distSqr = closestPointOnSegmentDistSqr(pos, a, b);
                if (distSqr < best) {
                    best = distSqr;
                    bestA = uuidA;
                    bestB = connUuid;
                }
            }
        }
        if (bestA != null && best < PICK_THRESHOLD_SQR) {
            return new String[]{bestA, bestB};
        }
        return null;
    }

    /**
     * Find a surface edge near the given position.
     * Returns [surfaceId] or null.
     */
    @Nullable
    private static String[] pickSurface(LocalPlayer player, Vec3 pos) {
        ListTag surfaces = PointDataStore.getSurfaces(player);
        double best = Double.MAX_VALUE;
        String bestId = null;
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
                double distSqr = closestPointOnSegmentDistSqr(pos, a, b);
                if (distSqr < best) {
                    best = distSqr;
                    bestId = surfId;
                }
            }
        }
        if (bestId != null && best < PICK_THRESHOLD_SQR) {
            return new String[]{bestId};
        }
        return null;
    }

    private static double closestPointOnSegmentDistSqr(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double lenSqr = ab.lengthSqr();
        if (lenSqr < 1e-10) return p.distanceToSqr(a);
        double t = p.subtract(a).dot(ab) / lenSqr;
        t = Math.max(0, Math.min(1, t));
        return a.add(ab.scale(t)).distanceToSqr(p);
    }

    private static CompoundTag findTagByUuid(ListTag list, String uuid) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            if (uuid.equals(tag.getString("id"))) return tag;
        }
        return null;
    }
}
