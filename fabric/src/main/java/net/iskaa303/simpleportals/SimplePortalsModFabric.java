package net.iskaa303.simpleportals;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.iskaa303.simpleportals.command.PortalCommand;
import net.iskaa303.simpleportals.config.SimplePortalsConfig;
import net.iskaa303.simpleportals.entity.PortalEntity;
import net.iskaa303.simpleportals.entity.PortalSyncPayload;
import net.iskaa303.simpleportals.entity.PortalWorldData;
import net.iskaa303.simpleportals.platform.SimplePortalsAgnos;
import net.minecraft.server.level.ServerPlayer;

public class SimplePortalsModFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimplePortalsConfig.load(SimplePortalsAgnos.getConfigDirectory());
        SimplePortalsMod.init();

        // Register command
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            PortalCommand.register(dispatcher);
        });

        // Sync all portals to joining players + populate server-side editor data
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            PortalWorldData data = PortalWorldData.get(player.serverLevel());
            for (PortalEntity portal : data.getAllPortals()) {
                SimplePortalsAgnos.syncPortalToPlayer(player, PortalSyncPayload.createPortal(portal));
                // Populate server-side PointDataStore for editing
                portal.populateEditorData(player);
            }
        });
    }
}
