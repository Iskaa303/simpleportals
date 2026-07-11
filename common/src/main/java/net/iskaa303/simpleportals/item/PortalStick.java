package net.iskaa303.simpleportals.item;

import net.iskaa303.simpleportals.client.gui.DragController;
import net.iskaa303.simpleportals.client.targeting.TargetSelector;
import net.iskaa303.simpleportals.entity.PortalEntity;
import net.iskaa303.simpleportals.entity.PortalSyncPayload;
import net.iskaa303.simpleportals.entity.PortalWorldData;
import net.iskaa303.simpleportals.platform.SimplePortalsAgnos;
import net.iskaa303.simpleportals.registry.SimplePortalsItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Portal Stick
 * Mode switched via the radial selection wheel.
 */
public class PortalStick extends Item {

    public PortalStick(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack == null) return null;

        // Suppress right-click action while client is dragging
        if (DragController.shouldCancelUse) {
            DragController.shouldCancelUse = false;
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide) {
            Vec3 target = TargetSelector.getCurrentTarget();
            if (target == null) return InteractionResultHolder.pass(stack);

            PortalStickMode mode = PointDataStore.getMode(player);

            switch (mode) {
                case POINT -> handlePointAction(player, target);
                case CONNECTION -> handleConnectionAction(level, player, target, stack);
                case SURFACE -> handleSurfaceAction(level, player, target);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    // ─── Point mode ───

    private void handlePointAction(@Nonnull Player player, @Nonnull Vec3 target) {
        PointDataStore.togglePoint(player, target);
    }

    // ─── Connection mode ───

    private void handleConnectionAction(@Nonnull Level level, @Nonnull Player player, @Nonnull Vec3 target, ItemStack stack) {
        String[] snappedConn = TargetSelector.getSnappedConnectionUuids();
        if (snappedConn != null) {
            PointDataStore.removeConnection(player, snappedConn[0], snappedConn[1]);
            player.displayClientMessage(Component.translatable("message.simpleportals.connection_removed"), true);
            return;
        }

        String pointUuid = PointDataStore.findPointUuid(player, target);
        if (pointUuid == null) {
            player.displayClientMessage(Component.translatable("message.simpleportals.no_point_at_cursor"), true);
            return;
        }

        String selectedUuid = PointDataStore.getSelectedEndpoint(player);

        if (selectedUuid.isEmpty()) {
            PointDataStore.setSelectedEndpoint(player, pointUuid);
            player.displayClientMessage(Component.translatable("message.simpleportals.endpoint_selected"), true);
        } else if (selectedUuid.equals(pointUuid)) {
            PointDataStore.setSelectedEndpoint(player, null);
            player.displayClientMessage(Component.translatable("message.simpleportals.endpoint_deselected"), true);
        } else {
            if (PointDataStore.hasConnection(player, selectedUuid, pointUuid)) {
                PointDataStore.removeConnection(player, selectedUuid, pointUuid);
                player.displayClientMessage(Component.translatable("message.simpleportals.connection_removed"), true);
            } else {
                PointDataStore.addConnection(player, selectedUuid, pointUuid);
                player.displayClientMessage(Component.translatable("message.simpleportals.connection_created"), true);
            }
            PointDataStore.setSelectedEndpoint(player, null);
        }
    }

    // ─── Surface mode ───

    private void handleSurfaceAction(@Nonnull Level level, @Nonnull Player player, @Nonnull Vec3 target) {
        // Check if looking at an existing portal entity — delete it
        if (level instanceof ServerLevel serverLevel) {
            PortalWorldData data = PortalWorldData.get(serverLevel);
            PortalEntity hitPortal = findPortalAt(player, target);
            if (hitPortal != null) {
                data.removePortal(hitPortal.getUuid());
                SimplePortalsAgnos.syncPortalToAll(serverLevel, PortalSyncPayload.deletePortal(hitPortal.getUuid()));
                player.displayClientMessage(Component.translatable("message.simpleportals.surface_removed"), true);
                return;
            }
        }

        // Find a cycle among editor points (from existing portals or user-created)
        String pointUuid = PointDataStore.findPointUuid(player, target);
        if (pointUuid == null) {
            player.displayClientMessage(Component.translatable("message.simpleportals.no_point_at_cursor"), true);
            return;
        }

        java.util.List<String> cycle = PointDataStore.findSmallestCycleContaining(player, pointUuid);
        if (cycle == null || cycle.size() < 3) {
            player.displayClientMessage(Component.translatable("message.simpleportals.no_loop_found"), true);
            return;
        }

        // Compute vertex positions
        var positions = PointDataStore.getPositionsByUuids(player, cycle);

        if (level instanceof ServerLevel serverLevel) {
            PortalWorldData data = PortalWorldData.get(serverLevel);

            // Save external connections from cycle points to non-cycle points
            // Map: externalPointUuid → list of cycle point UUIDs it's connected to
            java.util.Map<String, java.util.HashSet<String>> externalToCycle = new java.util.HashMap<>();
            java.util.Set<String> cycleSet = new java.util.HashSet<>(cycle);
            for (String cpUuid : cycle) {
                for (String conn : PointDataStore.getConnections(player, cpUuid)) {
                    if (!cycleSet.contains(conn)) {
                        externalToCycle.computeIfAbsent(conn, k -> new java.util.HashSet<>()).add(cpUuid);
                    }
                }
            }

            // Create portal entity
            java.util.Random rng = new java.util.Random();
            float[] color = {rng.nextFloat() * 0.8f + 0.2f, rng.nextFloat() * 0.8f + 0.2f, rng.nextFloat() * 0.8f + 0.2f};
            PortalEntity portal = PortalEntity.create(positions, color[0], color[1], color[2]);
            data.addPortal(portal);
            // Sync immediately — client will create editor points with random UUIDs
            SimplePortalsAgnos.syncPortalToAll(serverLevel, PortalSyncPayload.createPortal(portal));

            // Now reconnect external connections to the new portal's editor points
            // We need to know the mapping from old cycle point UUIDs to new portal editor point UUIDs.
            // The client generates these in populateEditorData. For the server side, we regenerate.
            var newEditorUuids = new java.util.ArrayList<String>();
            String puuid = portal.getUuid().toString();
            for (int i = 0; i < cycle.size(); i++) {
                String newUuid = java.util.UUID.randomUUID().toString();
                PointDataStore.addPointWithUuid(player, newUuid, positions.get(i));
                net.iskaa303.simpleportals.entity.PortalWorldData.registerEditorPoint(newUuid, puuid, i);
                newEditorUuids.add(newUuid);
            }
            // Connect consecutive new points
            for (int i = 0; i < cycle.size(); i++) {
                String a = newEditorUuids.get(i);
                String b = newEditorUuids.get((i + 1) % cycle.size());
                if (!PointDataStore.hasConnection(player, a, b)) {
                    PointDataStore.addConnection(player, a, b);
                }
            }
            // Reconnect external connections: for each connection from a cycle point to an external point,
            // replace it with a connection from the new editor point to the external point
            for (java.util.Map.Entry<String, java.util.HashSet<String>> entry : externalToCycle.entrySet()) {
                String extUuid = entry.getKey();
                for (String oldCycleUuid : entry.getValue()) {
                    int idx = cycle.indexOf(oldCycleUuid);
                    if (idx >= 0 && idx < newEditorUuids.size()) {
                        String newCycleUuid = newEditorUuids.get(idx);
                        // Remove old connection if it still exists
                        if (PointDataStore.hasConnection(player, oldCycleUuid, extUuid)) {
                            PointDataStore.removeConnection(player, oldCycleUuid, extUuid);
                        }
                        // Add new connection
                        if (!PointDataStore.hasConnection(player, newCycleUuid, extUuid)) {
                            PointDataStore.addConnection(player, newCycleUuid, extUuid);
                        }
                    }
                }
            }

            // Delete the original cycle points from PointDataStore
            for (String cpUuid : cycle) {
                PointDataStore.removePointByUuid(player, cpUuid);
            }
        }

        player.displayClientMessage(Component.translatable("message.simpleportals.surface_created"), true);
    }

    /** Find the nearest portal entity that the player is looking at. */
    @Nullable
    private static PortalEntity findPortalAt(Player player, Vec3 target) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return null;
        PortalWorldData data = PortalWorldData.get(serverLevel);
        Vec3 from = player.getEyePosition();
        Vec3 to = target;
        double reach = from.distanceTo(to);
        if (reach < 0.1) return null;
        PortalEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (PortalEntity portal : data.getAllPortals()) {
            if (portal.rayIntersects(from, to)) {
                double d = portal.getCentroid().distanceToSqr(from);
                if (d < bestDist) {
                    bestDist = d;
                    best = portal;
                }
            }
        }
        return best;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return use(context.getLevel(), context.getPlayer(), context.getHand()).getResult();
    }

    @Override
    public boolean canAttackBlock(@Nonnull BlockState state, @Nonnull Level level,
                                  @Nonnull net.minecraft.core.BlockPos pos, @Nonnull Player player) {
        return false;
    }

    @Nonnull
    public static ItemStack findPortalStick(@Nonnull Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.is(SimplePortalsItems.PORTAL_STICK.get())) return main;
        ItemStack off = player.getOffhandItem();
        if (off.is(SimplePortalsItems.PORTAL_STICK.get())) return off;
        for (int i = 0; i < 9; i++) {
            ItemStack hotbar = player.getInventory().getItem(i);
            if (hotbar.is(SimplePortalsItems.PORTAL_STICK.get())) return hotbar;
        }
        return ItemStack.EMPTY;
    }

    public static boolean isHolding(@Nullable Player player) {
        if (player == null) return false;
        var stick = SimplePortalsItems.PORTAL_STICK.get();
        if (stick == null) return false;
        ItemStack main = player.getMainHandItem();
        if (main.is(stick)) return true;
        ItemStack off = player.getOffhandItem();
        return off.is(stick);
    }
}
