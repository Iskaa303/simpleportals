package net.iskaa303.simpleportals.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import foundry.veil.api.client.render.rendertype.VeilRenderType;
import net.iskaa303.simpleportals.Constants;
import net.iskaa303.simpleportals.SimplePortalsMod;
import net.iskaa303.simpleportals.client.targeting.TargetSelector;
import net.iskaa303.simpleportals.registry.SimplePortalsItems;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;

/** Orchestrates the selection interface for Point Stick and Connection Stick. */
public class SelectionInterfaceRenderer {
    private static final ResourceLocation SELECTION_INTERFACE_RENDER_TYPE = SimplePortalsMod.path("selection_interface");

    private SelectionInterfaceRenderer() {}

    public static void render(@Nonnull PoseStack poseStack, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        var pointStick = SimplePortalsItems.POINT_STICK.get();
        var connStick = SimplePortalsItems.CONNECTION_STICK.get();
        if (pointStick == null && connStick == null) return;

        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        boolean hasPointStick = (pointStick != null && (main.is(pointStick) || off.is(pointStick)));
        boolean hasConnStick = (connStick != null && (main.is(connStick) || off.is(connStick)));
        if (!hasPointStick && !hasConnStick) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 eyePos = camera.getPosition();

        Vec3 targetPos = TargetSelector.getTarget(player, camera, partialTicks);
        if (targetPos == null) return;
        BlockHitResult hitResult = TargetSelector.getLastHitResult();

        RenderType renderType = VeilRenderType.get(SELECTION_INTERFACE_RENDER_TYPE, "simpleportals:item/point_stick");
        if (renderType == null) {
            Constants.LOG.error("Failed to get RenderType for selection interface", SELECTION_INTERFACE_RENDER_TYPE);
            return;
        }

        VertexConsumer builder = mc.renderBuffers().bufferSource().getBuffer(renderType);
        if (builder == null) return;

        poseStack.pushPose();
        poseStack.translate(-eyePos.x, -eyePos.y, -eyePos.z);

        Vec3[] connEndpoints = TargetSelector.getSnappedConnectionEndpoints();

        // Grid: hide when snapping to points (Ctrl) or connections (Shift + Connection Stick)
        boolean showGrid = !net.minecraft.client.gui.screens.Screen.hasControlDown()
                && !(player.isShiftKeyDown() && hasConnStick);
        if (showGrid) {
            GridRenderer.render(poseStack, eyePos, player.getViewVector(partialTicks),
                    hitResult, targetPos, builder);
        }
        CursorRenderer.render(poseStack, eyePos, targetPos, builder, connEndpoints);
        SavedPointsRenderer.render(poseStack, player, builder, eyePos);

        mc.renderBuffers().bufferSource().endBatch(renderType);
        poseStack.popPose();
    }
}
