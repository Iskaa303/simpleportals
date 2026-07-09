package net.iskaa303.simpleportals;

import net.fabricmc.api.ModInitializer;
import net.iskaa303.simpleportals.config.SimplePortalsConfig;
import net.iskaa303.simpleportals.platform.SimplePortalsAgnos;

public class SimplePortalsModFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimplePortalsConfig.load(SimplePortalsAgnos.getConfigDirectory());
        SimplePortalsMod.init();
    }
}
