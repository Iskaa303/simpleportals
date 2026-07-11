package net.iskaa303.simpleportals.entity;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-world persistent storage for PortalEntity instances.
 * Extends SavedData so portals survive world save/load.
 * Also exposes a static client-side map for rendering.
 */
public class PortalWorldData extends SavedData {

    private static final String DATA_NAME = "simpleportals_portals";
    private static final String PORTALS_KEY = "portals";
    private static final String CONNECTIONS_KEY = "portal_connections";

    private final Map<UUID, PortalEntity> portals = new HashMap<>();
    private final Set<String> connectedPairs = new HashSet<>(); // "uuidA:uuidB" sorted

    /** Client-side portal cache (populated by network packets). */
    public static final Map<UUID, PortalEntity> CLIENT_PORTALS = new ConcurrentHashMap<>();

    private PortalWorldData() {}

    // ─── Factory ───

    private static final SavedData.Factory<PortalWorldData> FACTORY = new SavedData.Factory<>(
            PortalWorldData::new,
            (tag, provider) -> load(tag),
            DataFixTypes.LEVEL
    );

    /** Get or create the PortalWorldData for the overworld. */
    @Nonnull
    public static PortalWorldData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(FACTORY, DATA_NAME);
    }

    // ─── Mutators ───

    public void addPortal(PortalEntity portal) {
        portals.put(portal.getUuid(), portal);
        setDirty();
    }

    public void removePortal(UUID uuid) {
        portals.remove(uuid);
        // Clean up connections involving this portal
        String uuidStr = uuid.toString();
        connectedPairs.removeIf(key -> key.startsWith(uuidStr + ":") || key.endsWith(":" + uuidStr));
        setDirty();
    }

    @Nullable
    public PortalEntity getPortal(UUID uuid) {
        return portals.get(uuid);
    }

    @Nonnull
    public Collection<PortalEntity> getAllPortals() {
        return Collections.unmodifiableCollection(portals.values());
    }

    public int getCount() { return portals.size(); }

    // ─── Portal-to-portal connections ───

    public void connectPortals(PortalEntity a, PortalEntity b) {
        String key = getConnectionKey(a, b);
        connectedPairs.add(key);
        // Assign same color to both
        float r = a.getR(), g = a.getG(), b2 = a.getB();
        PortalEntity newB = new PortalEntity(b.getUuid(), b.getVertices(), r, g, b2);
        portals.put(b.getUuid(), newB);
        setDirty();
    }

    public void disconnectPortals(PortalEntity a, PortalEntity b) {
        connectedPairs.remove(getConnectionKey(a, b));
        // Assign random colors to both
        portals.put(a.getUuid(), withRandomColor(a));
        portals.put(b.getUuid(), withRandomColor(b));
        setDirty();
    }

    /** Remove all linkages involving a portal UUID. */
    public void unlinkAllPortals(UUID portalUuid) {
        String uuidStr = portalUuid.toString();
        connectedPairs.removeIf(key -> key.startsWith(uuidStr + ":") || key.endsWith(":" + uuidStr));
        // Assign random colors to all affected portals
        for (var entry : portals.entrySet()) {
            portals.put(entry.getKey(), withRandomColor(entry.getValue()));
        }
        setDirty();
    }

    private static final java.util.Random COLOR_RNG = new java.util.Random();

    private static PortalEntity withRandomColor(PortalEntity p) {
        return new PortalEntity(p.getUuid(), p.getVertices(),
                COLOR_RNG.nextFloat() * 0.8f + 0.2f,
                COLOR_RNG.nextFloat() * 0.8f + 0.2f,
                COLOR_RNG.nextFloat() * 0.8f + 0.2f);
    }


    public boolean arePortalsConnected(PortalEntity a, PortalEntity b) {
        return connectedPairs.contains(getConnectionKey(a, b));
    }

    private static String getConnectionKey(PortalEntity a, PortalEntity b) {
        String u1 = a.getUuid().toString();
        String u2 = b.getUuid().toString();
        return u1.compareTo(u2) < 0 ? u1 + ":" + u2 : u2 + ":" + u1;
    }

    // ─── Serialization ───

    @Nonnull
    public static PortalWorldData load(CompoundTag tag) {
        PortalWorldData data = new PortalWorldData();
        ListTag list = tag.getList(PORTALS_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            PortalEntity portal = PortalEntity.load(list.getCompound(i));
            data.portals.put(portal.getUuid(), portal);
        }
        ListTag conns = tag.getList(CONNECTIONS_KEY, Tag.TAG_STRING);
        for (int i = 0; i < conns.size(); i++) {
            data.connectedPairs.add(conns.getString(i));
        }
        return data;
    }

    @Override
    @Nonnull
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (PortalEntity portal : portals.values()) {
            list.add(portal.save());
        }
        tag.put(PORTALS_KEY, list);
        ListTag conns = new ListTag();
        for (String key : connectedPairs) {
            conns.add(StringTag.valueOf(key));
        }
        tag.put(CONNECTIONS_KEY, conns);
        return tag;
    }

    // ─── Editor point tracking (point UUID → portal UUID + vertex index) ───

    private static final Map<String, String> pointToPortalUuid = new ConcurrentHashMap<>();
    private static final Map<String, Integer> pointToVertexIndex = new ConcurrentHashMap<>();

    /** Register that an editor point belongs to a portal vertex. */
    public static void registerEditorPoint(String pointUuid, String portalUuid, int vertexIndex) {
        pointToPortalUuid.put(pointUuid, portalUuid);
        pointToVertexIndex.put(pointUuid, vertexIndex);
    }

    @Nullable
    public static String getPortalUuidForPoint(String pointUuid) {
        return pointToPortalUuid.get(pointUuid);
    }

    public static int getVertexIndexForPoint(String pointUuid) {
        return pointToVertexIndex.getOrDefault(pointUuid, -1);
    }

    /** Update client-side portal vertex after a drag. */
    public static void updatePortalVertex(String portalUuid, int vertexIndex, Vec3 newPos) {
        UUID uuid = java.util.UUID.fromString(portalUuid);
        PortalEntity portal = CLIENT_PORTALS.get(uuid);
        if (portal != null) {
            CLIENT_PORTALS.put(uuid, updateVertex(portal, vertexIndex, newPos));
        }
    }

    /** Remove all editor data from PointDataStore for a player (before repopulating). */
    public static void clearEditorDataForPlayer(net.minecraft.world.entity.player.Player player) {
        for (String ptUuid : pointToPortalUuid.keySet()) {
            net.iskaa303.simpleportals.item.PointDataStore.removePointByUuid(player, ptUuid);
        }
        pointToPortalUuid.clear();
        pointToVertexIndex.clear();
    }

    private static PortalEntity updateVertex(PortalEntity portal, int idx, Vec3 pos) {
        var verts = new java.util.ArrayList<>(portal.getVertices());
        if (idx >= 0 && idx < verts.size()) {
            verts.set(idx, pos);
        }
        return new PortalEntity(portal.getUuid(), verts, portal.getR(), portal.getG(), portal.getB());
    }

    public static void addClientPortal(PortalEntity portal) {
        CLIENT_PORTALS.put(portal.getUuid(), portal);
    }

    public static void removeClientPortal(UUID uuid) {
        CLIENT_PORTALS.remove(uuid);
        // Also remove tracking for any points of this portal
        pointToPortalUuid.entrySet().removeIf(e -> e.getValue().equals(uuid.toString()));
        pointToVertexIndex.keySet().removeIf(k -> !pointToPortalUuid.containsKey(k));
    }

    public static void clearClientPortals() {
        CLIENT_PORTALS.clear();
        pointToPortalUuid.clear();
        pointToVertexIndex.clear();
    }
}
