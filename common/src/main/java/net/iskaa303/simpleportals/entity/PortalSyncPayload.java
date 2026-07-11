package net.iskaa303.simpleportals.entity;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Custom packet payload for syncing portal state between server and clients.
 * Supports CREATE, DELETE, and UPDATE actions.
 */
public record PortalSyncPayload(
        Action action,
        UUID uuid,
        List<Vec3> vertices,
        float r,
        float g,
        float b
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PortalSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    "simpleportals", "portal_sync"));

    public enum Action {
        CREATE,
        DELETE,
        UPDATE
    }

    public static PortalSyncPayload createPortal(PortalEntity portal) {
        return new PortalSyncPayload(Action.CREATE, portal.getUuid(), portal.getVertices(),
                portal.getR(), portal.getG(), portal.getB());
    }

    public static PortalSyncPayload updatePortal(PortalEntity portal) {
        return new PortalSyncPayload(Action.UPDATE, portal.getUuid(), portal.getVertices(),
                portal.getR(), portal.getG(), portal.getB());
    }

    public static PortalSyncPayload deletePortal(UUID uuid) {
        return new PortalSyncPayload(Action.DELETE, uuid, List.of(), 0, 0, 0);
    }

    public static final StreamCodec<ByteBuf, PortalSyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public PortalSyncPayload decode(ByteBuf buf) {
            return read(buf);
        }

        @Override
        public void encode(ByteBuf buf, PortalSyncPayload payload) {
            payload.write(buf);
        }
    };

    private static PortalSyncPayload read(ByteBuf buf) {
        Action action = Action.values()[buf.readByte()];
        UUID uuid = new UUID(buf.readLong(), buf.readLong());
        int count = buf.readInt();
        List<Vec3> vertices = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            vertices.add(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
        }
        float r = buf.readFloat();
        float g = buf.readFloat();
        float b = buf.readFloat();
        return new PortalSyncPayload(action, uuid, vertices, r, g, b);
    }

    private void write(ByteBuf buf) {
        buf.writeByte(action.ordinal());
        buf.writeLong(uuid.getMostSignificantBits());
        buf.writeLong(uuid.getLeastSignificantBits());
        buf.writeInt(vertices.size());
        for (Vec3 v : vertices) {
            buf.writeDouble(v.x);
            buf.writeDouble(v.y);
            buf.writeDouble(v.z);
        }
        buf.writeFloat(r);
        buf.writeFloat(g);
        buf.writeFloat(b);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
