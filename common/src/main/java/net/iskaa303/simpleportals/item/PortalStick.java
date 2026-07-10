package net.iskaa303.simpleportals.item;

import net.iskaa303.simpleportals.client.gui.DragController;
import net.iskaa303.simpleportals.client.targeting.TargetSelector;
import net.iskaa303.simpleportals.registry.SimplePortalsItems;
import net.minecraft.network.chat.Component;
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
                case SURFACE -> handleSurfaceAction(player, target);
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
        // If snapped to a connection, delete it on click
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

    private void handleSurfaceAction(@Nonnull Player player, @Nonnull Vec3 target) {
        // If snapped to a surface edge, delete it on click
        String snappedSurfaceId = TargetSelector.getSnappedSurfaceId();
        if (snappedSurfaceId != null) {
            PointDataStore.removeSurface(player, snappedSurfaceId);
            player.displayClientMessage(Component.translatable("message.simpleportals.surface_removed"), true);
            return;
        }

        String pointUuid = PointDataStore.findPointUuid(player, target);
        if (pointUuid == null) {
            player.displayClientMessage(Component.translatable("message.simpleportals.no_point_at_cursor"), true);
            return;
        }

        List<String> cycle = PointDataStore.findSmallestCycleContaining(player, pointUuid);
        if (cycle == null || cycle.size() < 3) {
            player.displayClientMessage(Component.translatable("message.simpleportals.no_loop_found"), true);
            return;
        }

        PointDataStore.addSurface(player, cycle);
        player.displayClientMessage(Component.translatable("message.simpleportals.surface_created"), true);
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
