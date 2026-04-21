package net.iskaa303.simpleportals;

import com.mojang.blaze3d.vertex.PoseStack;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.iskaa303.simpleportals.client.render.SelectionBoxRenderer;

public class SimplePortalsModFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        WorldRenderEvents.AFTER_ENTITIES.register((context) -> {
            PoseStack poseStack = context.matrixStack();
            if (poseStack == null) return;
            float partialTicks = context.tickCounter().getGameTimeDeltaTicks();
            SelectionBoxRenderer.render(poseStack, partialTicks);
        });
    }
}
