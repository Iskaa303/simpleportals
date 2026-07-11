package net.iskaa303.simpleportals.platform;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.iskaa303.simpleportals.Constants;
import net.iskaa303.simpleportals.entity.PortalSyncPayload;
import net.minecraft.server.level.ServerLevel;

import java.nio.file.Path;

public class SimplePortalsAgnosFabric extends SimplePortalsAgnos {

    static {
        SimplePortalsAgnos.delegate = new SimplePortalsAgnosFabric();
    }

    @Override
    protected Path getConfigDirectoryAgnos() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    protected void registerPortalPayloadsAgnos() {
        PayloadTypeRegistry.playS2C().register(PortalSyncPayload.TYPE, PortalSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(PortalSyncPayload.TYPE, PortalSyncPayload.STREAM_CODEC);
    }

    @Override
    protected void registerClientPortalHandlersAgnos() {
        ClientPlayNetworking.registerGlobalReceiver(PortalSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> SimplePortalsAgnos.handleClientPayload(payload));
        });
    }

    @Override
    protected void registerServerPortalHandlersAgnos() {
        ServerPlayNetworking.registerGlobalReceiver(PortalSyncPayload.TYPE, (payload, context) -> {
            ServerLevel level = context.player().serverLevel();
            context.server().execute(() -> SimplePortalsAgnos.handleServerPayload(payload, level));
        });
    }
}
