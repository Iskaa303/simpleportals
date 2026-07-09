package net.iskaa303.simpleportals.item;

import net.iskaa303.simpleportals.client.targeting.TargetSelector;
import net.iskaa303.simpleportals.registry.SimplePortalsItems;
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

/** Point Stick — creates/removes points in player data. */
public class PointStick extends Item {

    public PointStick(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack == null) return null;

        if (!level.isClientSide) {
            Vec3 target = TargetSelector.getCurrentTarget();
            if (target == null) return InteractionResultHolder.pass(stack);
            PointDataStore.togglePoint(player, target);
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

    /** Find Point Stick in hands or hotbar. */
    @Nonnull
    public static ItemStack findPointStick(@Nonnull Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.is(SimplePortalsItems.POINT_STICK.get())) return main;
        ItemStack off = player.getOffhandItem();
        if (off.is(SimplePortalsItems.POINT_STICK.get())) return off;
        for (int i = 0; i < 9; i++) {
            ItemStack hotbar = player.getInventory().getItem(i);
            if (hotbar.is(SimplePortalsItems.POINT_STICK.get())) return hotbar;
        }
        return ItemStack.EMPTY;
    }
}
