package net.iskaa303.simpleportals;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.iskaa303.simpleportals.config.ConfigData;

public class SimplePortalsModFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ConfigData.load(FabricLoader.getInstance().getConfigDir());
        SimplePortalsMod.init();
    }
}
