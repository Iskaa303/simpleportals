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
import javax.annotation.Nullable;
import java.util.List;

/** Surface Stick — creates/deletes surfaces from closed loops in the connection graph. */
public class SurfaceStick extends Item {

    public SurfaceStick(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack == null) return null;

        if (!level.isClientSide) {
            Vec3 target = TargetSelector.getCurrentTarget();
            if (target == null) return InteractionResultHolder.pass(stack);

            // If snapped to a surface edge (Shift held), delete it on click
            String snappedSurfaceId = TargetSelector.getSnappedSurfaceId();
            if (snappedSurfaceId != null) {
                PointDataStore.removeSurface(player, snappedSurfaceId);
                player.displayClientMessage(Component.translatable("message.simpleportals.surface_removed"), true);
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }

            String pointUuid = PointDataStore.findPointUuid(player, target);
            if (pointUuid == null) {
                player.displayClientMessage(Component.translatable("message.simpleportals.no_point_at_cursor"), true);
                return InteractionResultHolder.pass(stack);
            }

            List<String> cycle = PointDataStore.findSmallestCycleContaining(player, pointUuid);
            if (cycle == null || cycle.size() < 3) {
                player.displayClientMessage(Component.translatable("message.simpleportals.no_loop_found"), true);
                return InteractionResultHolder.pass(stack);
            }

            PointDataStore.addSurface(player, cycle);
            // ponytail: consume the cycle connections, leave points intact for reuse
            int n = cycle.size();
            for (int i = 0; i < n; i++) {
                PointDataStore.removeConnection(player, cycle.get(i), cycle.get((i + 1) % n));
            }
            player.displayClientMessage(Component.translatable("message.simpleportals.surface_created"), true);
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
