package net.iskaa303.simpleportals;

import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;

import net.iskaa303.simpleportals.client.render.SelectionInterfaceRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = Constants.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class SimplePortalsModNeoforgeClient {
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            PoseStack poseStack = event.getPoseStack();
            if (poseStack == null) return;

            poseStack.pushPose();
            try {
                Matrix4f modelViewMatrix = event.getModelViewMatrix();
                if (modelViewMatrix == null) return;
                poseStack.mulPose(modelViewMatrix);

                float partialTicks = event.getPartialTick().getGameTimeDeltaTicks();
                SelectionInterfaceRenderer.render(poseStack, partialTicks);
            } finally {
                poseStack.popPose();
            }
        }
    }
}
