package net.iskaa303.simpleportals.client.gui;

import net.iskaa303.simpleportals.client.keybinds.SimplePortalsKeybinds;
import net.iskaa303.simpleportals.client.targeting.TargetSelector;
import net.iskaa303.simpleportals.item.PointDataStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages surface copy and connect/disconnect operations triggered by key presses
 * while a surface is under the cursor.
 */
public final class SurfaceTransformController {

    // Edge detection
    private static boolean wasCopyDown = false;
    private static boolean wasConnectDown = false;

    private SurfaceTransformController() {}

    /** Called every client tick. */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        boolean holding = net.iskaa303.simpleportals.item.PortalStick.isHolding(player);
        if (!holding) {
            resetEdgeDetection();
            return;
        }

        String surfaceAtCursor = findSurfaceAtCursor(player);

        // --- Copy (C) ---
        boolean copyDown = SimplePortalsKeybinds.isDown(SimplePortalsKeybinds.getCopySurface());
        if (copyDown && !wasCopyDown && surfaceAtCursor != null) {
            String newId = PointDataStore.copySurface(player, surfaceAtCursor);
            if (!newId.isEmpty()) {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("message.simpleportals.surface_copied"), true);
            }
        }
        wasCopyDown = copyDown;

        // --- Connect/Disconnect (Z) ---
        boolean connectDown = SimplePortalsKeybinds.isDown(SimplePortalsKeybinds.getConnectSurface());
        if (connectDown && !wasConnectDown) {
            handleConnect(player, surfaceAtCursor);
        }
        wasConnectDown = connectDown;
    }

    // ─── Public queries for overlay hints ───

    /** Returns a surface ID if the cursor is near a surface, else null. */
    @Nullable
    public static String getSurfaceAtCursor() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        return findSurfaceAtCursor((LocalPlayer) mc.player);
    }

    /** True if the cursor is near any surface (for overlay hints). */
    public static boolean isNearSurface() {
        return getSurfaceAtCursor() != null;
    }

    // ─── Private helpers ───

    private static void resetEdgeDetection() {
        wasCopyDown = false;
        wasConnectDown = false;
    }

    /** Find surface whose points are closest to cursor. */
    @Nullable
    private static String findSurfaceAtCursor(LocalPlayer player) {
        Vec3 target = TargetSelector.getCurrentTarget();
        if (target == null) return null;
        var surfaces = PointDataStore.getSurfaces(player);
        double best = Double.MAX_VALUE;
        String bestId = null;
        for (int i = 0; i < surfaces.size(); i++) {
            var s = surfaces.getCompound(i);
            String id = s.getString("surface_id");
            var pts = s.getList("points", net.minecraft.nbt.Tag.TAG_STRING);
            for (int j = 0; j < pts.size(); j++) {
                Vec3 p = PointDataStore.getPointPosByUuid(player, pts.getString(j));
                if (p == null) continue;
                double d = p.distanceToSqr(target);
                if (d < best) {
                    best = d;
                    bestId = id;
                }
            }
        }
        if (bestId != null && best < 9.0) return bestId; // within 3 blocks
        return null;
    }

    // ─── Connect ───

    private static void handleConnect(LocalPlayer player, @Nullable String surfaceAtCursor) {
        if (surfaceAtCursor == null) return;
        String otherId = findNearestSurface(player, surfaceAtCursor);
        if (otherId == null) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("message.simpleportals.no_surface_nearby"), true);
            return;
        }
        if (!PointDataStore.hasSameShape(player, surfaceAtCursor, otherId)) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("message.simpleportals.surface_shape_mismatch"), true);
            return;
        }
        if (PointDataStore.areSurfacesConnected(player, surfaceAtCursor, otherId)) {
            PointDataStore.disconnectSurfaces(player, surfaceAtCursor, otherId);
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("message.simpleportals.surface_disconnected"), true);
        } else {
            PointDataStore.connectSurfaces(player, surfaceAtCursor, otherId);
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("message.simpleportals.surface_connected"), true);
        }
    }

    @Nullable
    private static String findNearestSurface(LocalPlayer player, String excludeId) {
        var surfaces = PointDataStore.getSurfaces(player);
        Vec3 target = TargetSelector.getCurrentTarget();
        if (target == null) return null;
        double best = Double.MAX_VALUE;
        String bestId = null;
        for (int i = 0; i < surfaces.size(); i++) {
            var s = surfaces.getCompound(i);
            String id = s.getString("surface_id");
            if (id.equals(excludeId)) continue;
            var pts = s.getList("points", net.minecraft.nbt.Tag.TAG_STRING);
            for (int j = 0; j < pts.size(); j++) {
                Vec3 p = PointDataStore.getPointPosByUuid(player, pts.getString(j));
                if (p == null) continue;
                double d = p.distanceToSqr(target);
                if (d < best) {
                    best = d;
                    bestId = id;
                }
            }
        }
        return (bestId != null && best < 9.0) ? bestId : null;
    }
}
