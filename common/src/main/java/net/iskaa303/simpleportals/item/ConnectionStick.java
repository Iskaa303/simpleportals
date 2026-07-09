package net.iskaa303.simpleportals.item;

import net.iskaa303.simpleportals.client.targeting.TargetSelector;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;

/** Connection Stick — manages connections between points. Data is per-player, no extra items needed. */
public class ConnectionStick extends Item {

    public ConnectionStick(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack == null) return null;

        if (!level.isClientSide) {
            Vec3 target = TargetSelector.getCurrentTarget();
            if (target == null) return InteractionResultHolder.pass(stack);

            // If snapped to a connection (Shift held), delete it on click
            String[] snappedConn = TargetSelector.getSnappedConnectionUuids();
            if (snappedConn != null) {
                PointDataStore.removeConnection(player, snappedConn[0], snappedConn[1]);
                player.displayClientMessage(Component.literal("§cConnection removed"), true);
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }

            String pointUuid = PointDataStore.findPointUuid(player, target);
            if (pointUuid == null) {
                player.displayClientMessage(Component.literal("§7No point at cursor — use Point Stick to create one"), true);
                return InteractionResultHolder.pass(stack);
            }

            String selectedUuid = PointDataStore.getSelectedEndpoint(player);

            if (selectedUuid.isEmpty()) {
                PointDataStore.setSelectedEndpoint(player, pointUuid);
                player.displayClientMessage(Component.literal("§eEndpoint selected"), true);
            } else if (selectedUuid.equals(pointUuid)) {
                PointDataStore.setSelectedEndpoint(player, null);
                player.displayClientMessage(Component.literal("§cEndpoint deselected"), true);
            } else {
                if (PointDataStore.hasConnection(player, selectedUuid, pointUuid)) {
                    PointDataStore.removeConnection(player, selectedUuid, pointUuid);
                    player.displayClientMessage(Component.literal("§cConnection removed"), true);
                } else {
                    PointDataStore.addConnection(player, selectedUuid, pointUuid);
                    player.displayClientMessage(Component.literal("§aConnection created"), true);
                }
                PointDataStore.setSelectedEndpoint(player, null);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
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
}
