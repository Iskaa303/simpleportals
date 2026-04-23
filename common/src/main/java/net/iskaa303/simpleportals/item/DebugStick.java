package net.iskaa303.simpleportals.item;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import net.iskaa303.simpleportals.client.render.SelectionInterfaceRenderer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class DebugStick extends Item {
    public static final String POINTS_KEY = "simpleportals_points_key";

    public DebugStick(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack == null) return null;
        
        if (!level.isClientSide) {
            Vec3 target = SelectionInterfaceRenderer.getCurrentTarget();
            if (target == null) return InteractionResultHolder.pass(stack);
            togglePoint(stack, player, target);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public void togglePoint(ItemStack stack, Player player, @Nonnull Vec3 targetPos) {
        DataComponentType<CustomData> customDataComponent = DataComponents.CUSTOM_DATA;
        CustomData customDataEmpty = CustomData.EMPTY;
        if (customDataComponent == null || customDataEmpty == null) return;
        stack.update(customDataComponent, customDataEmpty, customData -> customData.update(tag -> {
            ListTag list = tag.getList(POINTS_KEY, Tag.TAG_COMPOUND);
            
            // Check if point exists (with a small epsilon for double precision)
            int existingIndex = -1;
            for (int i = 0; i < list.size(); i++) {
                CompoundTag pTag = list.getCompound(i);
                Vec3 stored = new Vec3(pTag.getDouble("x"), pTag.getDouble("y"), pTag.getDouble("z"));
                if (stored.distanceToSqr(targetPos) < 0.0001) {
                    existingIndex = i;
                    break;
                }
            }

            if (existingIndex != -1) {
                list.remove(existingIndex);
                Component message = Component.literal("§cPoint Removed");
                if (message == null) return;
                player.displayClientMessage(message, true);
            } else {
                CompoundTag pTag = new CompoundTag();
                pTag.putDouble("x", targetPos.x);
                pTag.putDouble("y", targetPos.y);
                pTag.putDouble("z", targetPos.z);
                list.add(pTag);
                Component message = Component.literal("§bPoint Created");
                if (message == null) return;
                player.displayClientMessage(message, true);
            }
            tag.put(POINTS_KEY, list);
        }));
    }

    public static List<Vec3> getPoints(ItemStack stack) {
        List<Vec3> points = new ArrayList<>();
        DataComponentType<CustomData> customData = DataComponents.CUSTOM_DATA;
        if (customData == null) return points;
        CustomData data = stack.get(customData);
        if (data != null) {
            CompoundTag tag = data.copyTag();
            ListTag list = tag.getList(POINTS_KEY, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag pTag = list.getCompound(i);
                points.add(new Vec3(pTag.getDouble("x"), pTag.getDouble("y"), pTag.getDouble("z")));
            }
        }
        return points;
    }
}
