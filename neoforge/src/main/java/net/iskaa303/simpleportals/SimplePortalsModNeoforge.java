package net.iskaa303.simpleportals;

import net.iskaa303.simpleportals.config.ConfigData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;

@Mod(Constants.MOD_ID)
public class SimplePortalsModNeoforge {
    public SimplePortalsModNeoforge(IEventBus eventBus) {
        ConfigData.load(FMLPaths.CONFIGDIR.get());
        SimplePortalsMod.init();
    }
}