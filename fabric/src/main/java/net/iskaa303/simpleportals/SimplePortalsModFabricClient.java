package net.iskaa303.simpleportals;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.iskaa303.simpleportals.client.gui.ControlsOverlay;
import net.iskaa303.simpleportals.client.gui.ModeSelectionOverlay;
import net.iskaa303.simpleportals.client.render.SelectionInterfaceRenderer;

public class SimplePortalsModFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        WorldRenderEvents.AFTER_ENTITIES.register((context) -> {
            PoseStack poseStack = context.matrixStack();
            if (poseStack == null) return;
            float partialTicks = context.tickCounter().getGameTimeDeltaTicks();
            SelectionInterfaceRenderer.render(poseStack, partialTicks);
        });

        HudRenderCallback.EVENT.register((guiGraphics, deltaTracker) -> {
            ControlsOverlay.render(guiGraphics);
            ModeSelectionOverlay.renderOverlay(guiGraphics, deltaTracker);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            try {
                ModeSelectionOverlay.tick();
            } catch (Exception e) {
                Constants.LOG.error("Mode wheel tick error", e);
            }
        });
    }
}
