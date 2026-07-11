package net.iskaa303.simpleportals.platform;

import net.iskaa303.simpleportals.entity.PortalEntity;
import net.iskaa303.simpleportals.entity.PortalSyncPayload;
import net.iskaa303.simpleportals.entity.PortalWorldData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;

import java.nio.file.Path;

/**
 * Platform abstraction.
 * Provides loader-specific services without common code knowing the loader.
 */
public abstract class SimplePortalsAgnos {

    public static SimplePortalsAgnos delegate;

    static {
        try {
            Class.forName("net.iskaa303.simpleportals.platform.SimplePortalsAgnosFabric");
        } catch (Throwable ignored) {}
        try {
            Class.forName("net.iskaa303.simpleportals.platform.SimplePortalsAgnosNeoForge");
        } catch (Throwable ignored) {}
    }

    public static Path getConfigDirectory() {
        return delegate.getConfigDirectoryAgnos();
    }

    public static void registerPortalPayloads() {
        if (delegate != null) delegate.registerPortalPayloadsAgnos();
    }

    public static void registerClientPortalHandlers() {
        if (delegate != null) delegate.registerClientPortalHandlersAgnos();
    }

    public static void registerServerPortalHandlers() {
        if (delegate != null) delegate.registerServerPortalHandlersAgnos();
    }

    // ─── Common helpers ───

    public static void syncPortalToAll(ServerLevel level, PortalSyncPayload payload) {
        ClientboundCustomPayloadPacket packet = new ClientboundCustomPayloadPacket(payload);
        for (ServerPlayer player : level.players()) {
            player.connection.send(packet);
        }
    }

    public static void syncPortalToPlayer(ServerPlayer player, PortalSyncPayload payload) {
        player.connection.send(new ClientboundCustomPayloadPacket(payload));
    }

    /** Handle a received portal sync payload on the client. */
    public static void handleClientPayload(PortalSyncPayload payload) {
        switch (payload.action()) {
            case CREATE, UPDATE -> {
                var portal = new PortalEntity(payload.uuid(), payload.vertices(), payload.r(), payload.g(), payload.b());
                PortalWorldData.addClientPortal(portal);
                // Editor data (PointDataStore) is already managed by the server side.
                // Don't regenerate it here — that would break external connections.
            }
            case DELETE -> PortalWorldData.removeClientPortal(payload.uuid());
        }
    }

    /** Handle a received portal sync payload on the server. */
    public static void handleServerPayload(PortalSyncPayload payload, ServerLevel level) {
        PortalWorldData data = PortalWorldData.get(level);
        switch (payload.action()) {
            case UPDATE -> {
                var portal = new PortalEntity(payload.uuid(), payload.vertices(), payload.r(), payload.g(), payload.b());
                data.addPortal(portal);
                syncPortalToAll(level, PortalSyncPayload.updatePortal(portal));
            }
        }
    }

    // ─── Abstract methods ───

    protected abstract Path getConfigDirectoryAgnos();
    protected abstract void registerPortalPayloadsAgnos();
    protected abstract void registerClientPortalHandlersAgnos();
    protected abstract void registerServerPortalHandlersAgnos();
}
