package net.iskaa303.simpleportals.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import foundry.veil.api.client.render.rendertype.VeilRenderType;
import net.iskaa303.simpleportals.Constants;
import net.iskaa303.simpleportals.SimplePortalsMod;
import net.iskaa303.simpleportals.client.keybinds.SimplePortalsKeybinds;
import net.iskaa303.simpleportals.client.targeting.TargetSelector;
import net.iskaa303.simpleportals.item.PortalStickMode;
import net.iskaa303.simpleportals.item.PointDataStore;
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

/** Orchestrates the selection interface for the Portal Stick. */
public class SelectionInterfaceRenderer {
    private static final ResourceLocation SELECTION_INTERFACE_RENDER_TYPE = SimplePortalsMod.path("selection_interface");

    private SelectionInterfaceRenderer() {}

    public static void render(@Nonnull PoseStack poseStack, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        var stick = SimplePortalsItems.PORTAL_STICK.get();
        if (stick == null) return;

        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        if (!main.is(stick) && !off.is(stick)) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 eyePos = camera.getPosition();

        Vec3 targetPos = TargetSelector.getTarget(player, camera, partialTicks);
        if (targetPos == null) return;
        BlockHitResult hitResult = TargetSelector.getLastHitResult();

        RenderType renderType = VeilRenderType.get(SELECTION_INTERFACE_RENDER_TYPE, "simpleportals:item/portal_stick");
        if (renderType == null) {
            Constants.LOG.error("Failed to get RenderType for selection interface", SELECTION_INTERFACE_RENDER_TYPE);
            return;
        }

        VertexConsumer builder = mc.renderBuffers().bufferSource().getBuffer(renderType);
        if (builder == null) return;

        poseStack.pushPose();
        poseStack.translate(-eyePos.x, -eyePos.y, -eyePos.z);

        Vec3[] connEndpoints = TargetSelector.getSnappedConnectionEndpoints();
        Vec3[] surfEndpoints = TargetSelector.getSnappedSurfaceEndpoints();

        PortalStickMode mode = PointDataStore.getMode(player);

        // Grid: in Point mode always show; in other modes hide while snapping
        boolean snapToPoint = SimplePortalsKeybinds.isDown(SimplePortalsKeybinds.getSnapPoint());
        boolean snapToGrid = SimplePortalsKeybinds.isDown(SimplePortalsKeybinds.getSnapGrid());
        boolean showGrid = mode == PortalStickMode.POINT || (!snapToPoint && !snapToGrid);
        if (showGrid) {
            GridRenderer.render(poseStack, eyePos, player.getViewVector(partialTicks),
                    hitResult, targetPos, builder);
        }

        Vec3[] cursorEndpoints = connEndpoints != null ? connEndpoints : surfEndpoints;
        CursorRenderer.render(poseStack, eyePos, targetPos, builder, cursorEndpoints, TargetSelector.getSnappedSurfaceVertices());
        SavedPointsRenderer.render(poseStack, player, builder, eyePos);
        if (mode == PortalStickMode.SURFACE) {
            SurfaceRenderer.render(poseStack, player, builder, eyePos);
        }

        mc.renderBuffers().bufferSource().endBatch(renderType);
        poseStack.popPose();
    }
}
