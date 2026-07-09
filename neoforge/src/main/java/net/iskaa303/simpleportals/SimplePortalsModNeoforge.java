package net.iskaa303.simpleportals;

import net.iskaa303.simpleportals.config.SimplePortalsConfig;
import net.iskaa303.simpleportals.platform.SimplePortalsAgnos;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class SimplePortalsModNeoforge {
    public SimplePortalsModNeoforge(IEventBus eventBus) {
        SimplePortalsConfig.load(SimplePortalsAgnos.getConfigDirectory());
        SimplePortalsMod.init();
    }
}