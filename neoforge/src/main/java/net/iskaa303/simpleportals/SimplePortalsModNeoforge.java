package net.iskaa303.simpleportals;

import net.iskaa303.simpleportals.command.PortalCommand;
import net.iskaa303.simpleportals.config.SimplePortalsConfig;
import net.iskaa303.simpleportals.platform.SimplePortalsAgnos;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;

@Mod(Constants.MOD_ID)
public class SimplePortalsModNeoforge {
    public SimplePortalsModNeoforge(IEventBus eventBus) {
        SimplePortalsConfig.load(SimplePortalsAgnos.getConfigDirectory());
        SimplePortalsMod.init();

        // Register commands on the game bus (not mod bus)
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        PortalCommand.register(event.getDispatcher());
    }
}
