package net.iskaa303.simpleportals.client.gui;

import net.iskaa303.simpleportals.client.keybinds.SimplePortalsKeybinds;
import net.iskaa303.simpleportals.client.targeting.TargetSelector;
import net.iskaa303.simpleportals.entity.PortalEntity;
import net.iskaa303.simpleportals.entity.PortalSyncPayload;
import net.iskaa303.simpleportals.entity.PortalWorldData;
import net.iskaa303.simpleportals.platform.SimplePortalsAgnos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manages portal copy, connect/disconnect, and unlink operations.
 * Uses the integrated server for singleplayer operations.
 */
public final class SurfaceTransformController {

    private static boolean wasCopyDown = false;
    private static boolean wasConnectDown = false;
    private static String selectedPortalUuid = null;

    private SurfaceTransformController() {}

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        boolean holding = net.iskaa303.simpleportals.item.PortalStick.isHolding(player);
        if (!holding) {
            resetEdgeDetection();
            return;
        }

        PortalEntity portalAtCursor = findPortalAtCursor(player);

        // --- Copy (C) ---
        boolean copyDown = SimplePortalsKeybinds.isDown(SimplePortalsKeybinds.getCopySurface());
        if (copyDown && !wasCopyDown && portalAtCursor != null) {
            handleCopy(player, portalAtCursor);
        }
        wasCopyDown = copyDown;

        // --- Connect (Z) and Unlink-all (Alt+Z) ---
        boolean connectDown = SimplePortalsKeybinds.isDown(SimplePortalsKeybinds.getConnectSurface());
        if (connectDown && !wasConnectDown) {
            if (Screen.hasAltDown() && portalAtCursor != null) {
                handleUnlinkAll(player, portalAtCursor);
            } else {
                handleConnect(player, portalAtCursor);
            }
        }
        wasConnectDown = connectDown;
    }

    // ─── Public queries for overlay hints ───

    @Nullable
    public static PortalEntity getPortalAtCursor() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        return findPortalAtCursor((LocalPlayer) mc.player);
    }

    public static boolean isNearSurface() {
        return getPortalAtCursor() != null;
    }

    @Nullable
    public static String getSelectedPortalUuid() {
        return selectedPortalUuid;
    }

    // ─── Integrated server access (singleplayer) ───

    @Nullable
    private static ServerLevel getServerLevel() {
        var mc = Minecraft.getInstance();
        var server = mc.getSingleplayerServer();
        return server != null ? server.overworld() : null;
    }

    // ─── Private helpers ───

    private static void resetEdgeDetection() {
        wasCopyDown = false;
        wasConnectDown = false;
    }

    @Nullable
    private static PortalEntity findPortalAtCursor(LocalPlayer player) {
        Vec3 target = TargetSelector.getCurrentTarget();
        if (target == null) return null;
        PortalEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (PortalEntity portal : PortalWorldData.CLIENT_PORTALS.values()) {
            Vec3 centroid = portal.getCentroid();
            double d = centroid.distanceToSqr(target);
            if (d < bestDist && d < 16.0) {
                bestDist = d;
                best = portal;
            }
        }
        return best;
    }

    // ─── Copy ───

    private static void handleCopy(LocalPlayer player, PortalEntity source) {
        ServerLevel level = getServerLevel();
        if (level == null) return;
        var data = PortalWorldData.get(level);
        var newPortal = PortalEntity.create(source.getVertices(), source.getR(), source.getG(), source.getB());
        data.addPortal(newPortal);
        SimplePortalsAgnos.syncPortalToAll(level, PortalSyncPayload.createPortal(newPortal));
        player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("message.simpleportals.surface_copied"), true);
    }

    // ─── Unlink all ───

    private static void handleUnlinkAll(LocalPlayer player, PortalEntity portal) {
        ServerLevel level = getServerLevel();
        if (level == null) return;
        var data = PortalWorldData.get(level);
        data.unlinkAllPortals(portal.getUuid());
        // Re-sync to update colors
        for (PortalEntity p : data.getAllPortals()) {
            SimplePortalsAgnos.syncPortalToAll(level, PortalSyncPayload.createPortal(p));
        }
        player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("message.simpleportals.surface_disconnected"), true);
    }

    // ─── Connect/Disconnect ───

    private static void handleConnect(LocalPlayer player, @Nullable PortalEntity portalAtCursor) {
        if (portalAtCursor == null) {
            if (selectedPortalUuid != null) {
                selectedPortalUuid = null;
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("message.simpleportals.endpoint_deselected"), true);
            }
            return;
        }

        String cursorUuid = portalAtCursor.getUuid().toString();

        if (selectedPortalUuid == null) {
            selectedPortalUuid = cursorUuid;
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("message.simpleportals.surface_selected"), true);
        } else if (selectedPortalUuid.equals(cursorUuid)) {
            selectedPortalUuid = null;
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("message.simpleportals.endpoint_deselected"), true);
        } else {
            // Two different portals — attempt connect/disconnect
            ServerLevel level = getServerLevel();
            if (level != null) {
                var data = PortalWorldData.get(level);
                PortalEntity portalA = data.getPortal(UUID.fromString(selectedPortalUuid));
                PortalEntity portalB = data.getPortal(UUID.fromString(cursorUuid));

                if (portalA != null && portalB != null) {
                    if (hasSameShape(portalA, portalB)) {
                        boolean connected = data.arePortalsConnected(portalA, portalB);
                        if (connected) {
                            data.disconnectPortals(portalA, portalB);
                            player.displayClientMessage(
                                    net.minecraft.network.chat.Component.translatable("message.simpleportals.surface_disconnected"), true);
                        } else {
                            data.connectPortals(portalA, portalB);
                            player.displayClientMessage(
                                    net.minecraft.network.chat.Component.translatable("message.simpleportals.surface_connected"), true);
                        }
                        // Re-sync both portals
                        SimplePortalsAgnos.syncPortalToAll(level, PortalSyncPayload.createPortal(
                                data.getPortal(portalA.getUuid())));
                        SimplePortalsAgnos.syncPortalToAll(level, PortalSyncPayload.createPortal(
                                data.getPortal(portalB.getUuid())));
                    } else {
                        player.displayClientMessage(
                                net.minecraft.network.chat.Component.translatable("message.simpleportals.surface_shape_mismatch"), true);
                    }
                }
            }
            selectedPortalUuid = null;
        }
    }

    /**
     * Shape comparison that handles scaling, rotation, and translation.
     * Compares normalized edge-length profiles.
     */
    /**
     * Shape comparison that handles scaling, rotation, translation, and reflection.
     * Projects both polygons into their own 2D planes, normalizes translation + scale,
     * then compares side lengths and internal angles.
     */
    /**
     * Polygon similarity using exact cyclic signature matching (0-error geometric approach).
     * Signature = interleaved [s1, θ1, s2, θ2, ...] where s are normalized edge lengths
     * and θ are interior angles. Normalization by first edge length handles scaling.
     */
    public static boolean hasSameShape(PortalEntity a, PortalEntity b) {
        var vertsA = a.getVertices();
        var vertsB = b.getVertices();
        int n = vertsA.size();
        if (n < 3 || n != vertsB.size()) return false;

        double[] sigA = buildSignature(vertsA);
        double[] sigB = buildSignature(vertsB);
        if (sigA == null || sigB == null) return false;

        // Cyclic matching: duplicate sigA, search for sigB within it
        int m = n * 2;
        double[] doubled = new double[m * 2]; // each element is [s_i, θ_i] pair
        for (int i = 0; i < m; i++) {
            doubled[i * 2] = sigA[(i % n) * 2];
            doubled[i * 2 + 1] = sigA[(i % n) * 2 + 1];
        }

        // Search for sigB in doubled (forward)
        if (containsSignature(doubled, n * 2, sigB, n * 2)) return true;

        // Search for reversed sigB (reflection)
        double[] revSigB = new double[n * 2];
        for (int i = 0; i < n; i++) {
            revSigB[i * 2] = sigB[((n - 1 - i) % n) * 2];         // reversed side
            revSigB[i * 2 + 1] = sigB[((n - 1 - i) % n) * 2 + 1]; // reversed angle
        }
        return containsSignature(doubled, n * 2, revSigB, n * 2);
    }

    /** Check if needle (length m) appears in haystack (length h) within epsilon tolerance. */
    private static boolean containsSignature(double[] haystack, int h, double[] needle, int m) {
        double eps = 1e-9;
        for (int start = 0; start <= h - m; start++) {
            boolean match = true;
            for (int i = 0; i < m; i++) {
                if (Math.abs(haystack[start + i] - needle[i]) > eps) {
                    match = false;
                    break;
                }
            }
            if (match) return true;
        }
        return false;
    }

    /** Build polygon signature: [s1, θ1, s2, θ2, ...] with edge lengths normalized by first edge. */
    @Nullable
    private static double[] buildSignature(java.util.List<Vec3> verts) {
        int n = verts.size();
        if (n < 3) return null;

        double[] sides = new double[n];
        for (int i = 0; i < n; i++) {
            sides[i] = verts.get(i).distanceTo(verts.get((i + 1) % n));
        }

        // Normalize by first edge length (handles scaling)
        double s0 = sides[0];
        if (s0 < 1e-10) return null;
        for (int i = 0; i < n; i++) sides[i] /= s0;

        double[] angles = new double[n];
        for (int i = 0; i < n; i++) {
            int prev = (i - 1 + n) % n;
            int next = (i + 1) % n;
            Vec3 a = verts.get(prev);
            Vec3 b = verts.get(i);
            Vec3 c = verts.get(next);
            Vec3 ba = a.subtract(b).normalize();
            Vec3 bc = c.subtract(b).normalize();
            double dot = ba.dot(bc);
            angles[i] = Math.acos(Math.max(-1, Math.min(1, dot)));
        }

        double[] sig = new double[n * 2];
        for (int i = 0; i < n; i++) {
            sig[i * 2] = sides[i];
            sig[i * 2 + 1] = angles[i];
        }
        return sig;
    }
}
