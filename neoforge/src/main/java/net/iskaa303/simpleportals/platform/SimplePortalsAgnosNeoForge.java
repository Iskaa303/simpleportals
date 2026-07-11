package net.iskaa303.simpleportals.platform;

import net.iskaa303.simpleportals.Constants;
import net.iskaa303.simpleportals.entity.PortalSyncPayload;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.nio.file.Path;

@EventBusSubscriber(modid = Constants.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class SimplePortalsAgnosNeoForge extends SimplePortalsAgnos {

    static {
        SimplePortalsAgnos.delegate = new SimplePortalsAgnosNeoForge();
    }

    @Override
    protected Path getConfigDirectoryAgnos() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    protected void registerPortalPayloadsAgnos() {}

    @Override
    protected void registerClientPortalHandlersAgnos() {}

    @Override
    protected void registerServerPortalHandlersAgnos() {}

    @SubscribeEvent
    public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(Constants.MOD_ID);
        registrar.playBidirectional(
                PortalSyncPayload.TYPE,
                PortalSyncPayload.STREAM_CODEC,
                (payload, context) -> {
                    // Determine direction from the connection
                    if (context.connection().getReceiving() == PacketFlow.CLIENTBOUND) {
                        // Packet is being received by the client → client-side handling
                        net.minecraft.client.Minecraft.getInstance().execute(() -> {
                            SimplePortalsAgnos.handleClientPayload(payload);
                        });
                    } else {
                        // Packet is being received by the server → server-side handling
                        var player = (net.minecraft.server.level.ServerPlayer) context.listener();
                        var level = player.serverLevel();
                        level.getServer().execute(() -> {
                            SimplePortalsAgnos.handleServerPayload(payload, level);
                        });
                    }
                }
        );
    }
}
