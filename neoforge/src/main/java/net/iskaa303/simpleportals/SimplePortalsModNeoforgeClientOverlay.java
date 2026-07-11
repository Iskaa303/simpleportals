package net.iskaa303.simpleportals;

import net.iskaa303.simpleportals.client.gui.ControlsOverlay;
import net.iskaa303.simpleportals.client.gui.ModeSelectionOverlay;
import net.iskaa303.simpleportals.client.gui.SimplePortalsConfigScreen;
import net.iskaa303.simpleportals.client.render.PortalRenderer;
import net.iskaa303.simpleportals.platform.SimplePortalsAgnos;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@EventBusSubscriber(modid = Constants.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SimplePortalsModNeoforgeClientOverlay {
    private SimplePortalsModNeoforgeClientOverlay() {}

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent evt) {
        evt.registerAboveAll(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "controls"),
                (guiGraphics, deltaTracker) -> ControlsOverlay.render(guiGraphics));
        evt.registerAboveAll(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "mode_wheel"),
                (guiGraphics, deltaTracker) -> ModeSelectionOverlay.renderOverlay(guiGraphics, deltaTracker));
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent evt) {
        // Register portal client networking
        SimplePortalsAgnos.registerClientPortalHandlers();

        ModList.get().getModContainerById(Constants.MOD_ID).ifPresent(container -> {
            container.registerExtensionPoint(IConfigScreenFactory.class,
                    (mc, screen) -> new SimplePortalsConfigScreen(screen));
        });

        NeoForge.EVENT_BUS.addListener((final ClientTickEvent.Post ev) -> {
            try {
                ModeSelectionOverlay.tick();
            } catch (Exception e) {
                Constants.LOG.error("Mode wheel tick error", e);
            }
        });

        // Register portal rendering in the level render stage
        NeoForge.EVENT_BUS.addListener((final RenderLevelStageEvent ev) -> {
            if (ev.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
                PortalRenderer.renderAll(ev.getPoseStack(), ev.getCamera());
            }
        });
    }
}
