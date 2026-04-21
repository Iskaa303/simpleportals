package net.iskaa303.simpleportals;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class SimplePortalsModNeoforge {
    public SimplePortalsModNeoforge(IEventBus eventBus) {
        SimplePortalsMod.init();
    }
}