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

public class SelectionInterfaceRenderer {
    private static final ResourceLocation SELECTION_INTERFACE_RENDER_TYPE = SimplePortalsMod.path("selection_interface");

    private SelectionInterfaceRenderer() {}

    /**
     * Orchestrates the full selection interface: computes the target via TargetSelector,
     * then delegates to sub-renderers for the grid, cursor box, and saved-point boxes.
     */
    public static void render(@Nonnull PoseStack poseStack, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        var stick = SimplePortalsItems.PORTAL_STICK.get();
        if (stick == null) return;
        ItemStack stickStack = player.getMainHandItem().is(stick) ? player.getMainHandItem() : player.getOffhandItem();
        if (!stickStack.is(stick)) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 eyePos = camera.getPosition();

        // Compute target through the selector (handles raycast, snap, smoothing)
        Vec3 targetPos = TargetSelector.getTarget(player, camera, partialTicks);
        if (targetPos == null) return;
        BlockHitResult hitResult = TargetSelector.getLastHitResult();

        // Resolve the Veil render type for this interface
        RenderType renderType = VeilRenderType.get(SELECTION_INTERFACE_RENDER_TYPE, "simpleportals:item/portal_stick");
        if (renderType == null) {
            Constants.LOG.error("Failed to get RenderType for selection interface", SELECTION_INTERFACE_RENDER_TYPE);
            return;
        }

        VertexConsumer builder = mc.renderBuffers().bufferSource().getBuffer(renderType);
        if (builder == null) return;

        poseStack.pushPose();
        poseStack.translate(-eyePos.x, -eyePos.y, -eyePos.z);

        GridRenderer.render(poseStack, eyePos, player.getViewVector(partialTicks),
                hitResult, targetPos, builder);
        CursorRenderer.render(poseStack, eyePos, targetPos, builder);
        SavedPointsRenderer.render(poseStack, stickStack, builder);

        mc.renderBuffers().bufferSource().endBatch(renderType);
        poseStack.popPose();
    }
}