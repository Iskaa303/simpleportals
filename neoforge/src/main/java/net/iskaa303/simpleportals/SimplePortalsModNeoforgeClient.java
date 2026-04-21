package net.iskaa303.simpleportals;

import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import com.mojang.blaze3d.vertex.PoseStack;

import net.iskaa303.simpleportals.client.render.SelectionBoxRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = Constants.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class SimplePortalsModNeoforgeClient {
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            PoseStack poseStack = event.getPoseStack();
            if (poseStack == null) return;

            float partialTicks = event.getPartialTick().getGameTimeDeltaTicks();
            SelectionBoxRenderer.render(poseStack, partialTicks);
        }
    }
}
