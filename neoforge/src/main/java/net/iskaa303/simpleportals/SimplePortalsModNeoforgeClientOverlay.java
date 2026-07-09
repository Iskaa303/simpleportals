package net.iskaa303.simpleportals;

import net.iskaa303.simpleportals.client.gui.ControlsOverlay;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(modid = Constants.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SimplePortalsModNeoforgeClientOverlay {
    private SimplePortalsModNeoforgeClientOverlay() {}


    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "controls"),
                (guiGraphics, deltaTracker) -> ControlsOverlay.render(guiGraphics));
    }
}
