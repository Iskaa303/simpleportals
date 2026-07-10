package net.iskaa303.simpleportals.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player data.
 */
public class PointDataStore {

    private static final String DATA_KEY = "simpleportals_data";
    private static final String POINTS_KEY = "points";
    private static final String ID_KEY = "id";
    private static final String X_KEY = "x";
    private static final String Y_KEY = "y";
    private static final String Z_KEY = "z";
    private static final String CONNECTIONS_KEY = "connections";
    private static final String SELECTED_KEY = "selected_endpoint";
    private static final String SURFACES_KEY = "surfaces";
    private static final String SURFACE_ID_KEY = "surface_id";
    private static final String SURFACE_POINTS_KEY = "points";
    private static final double EPSILON = 0.0001;
    private static final String MODE_KEY = "portal_stick_mode";

    private static final Map<UUID, CompoundTag> PLAYER_DATA = new ConcurrentHashMap<>();

    private PointDataStore() {}

    private static CompoundTag getData(@Nonnull Player player) {
        return PLAYER_DATA.computeIfAbsent(player.getUUID(), k -> new CompoundTag());
    }

    private static void setData(@Nonnull Player player, @Nonnull CompoundTag data) {
        PLAYER_DATA.put(player.getUUID(), data);
    }

    // ─── Mode persistence ───

    public static PortalStickMode getMode(@Nonnull Player player) {
        CompoundTag data = getData(player);
        if (data.contains(MODE_KEY, Tag.TAG_STRING)) {
            return PortalStickMode.byName(data.getString(MODE_KEY));
        }
        return PortalStickMode.POINT; // default
    }

    public static void setMode(@Nonnull Player player, @Nonnull PortalStickMode mode) {
        CompoundTag data = getData(player);
        data.putString(MODE_KEY, mode.getSerializedName());
        setData(player, data);
    }

    @Nonnull
    public static ListTag getPointList(@Nonnull Player player) {
        return getData(player).getList(POINTS_KEY, Tag.TAG_COMPOUND);
    }

    @Nonnull
    public static List<Vec3> getPoints(@Nonnull Player player) {
        List<Vec3> points = new ArrayList<>();
        ListTag list = getPointList(player);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            points.add(new Vec3(tag.getDouble(X_KEY), tag.getDouble(Y_KEY), tag.getDouble(Z_KEY)));
        }
        return points;
    }

    public static int getPointCount(@Nonnull Player player) {
        return getPointList(player).size();
    }

    public static CompoundTag getPointByUuid(@Nonnull Player player, @Nonnull String uuid) {
        ListTag list = getPointList(player);
        return findTagByUuid(list, uuid);
    }

    public static String findPointUuid(@Nonnull Player player, @Nonnull Vec3 pos) {
        return findPointUuid(player, pos, EPSILON);
    }

    public static String findPointUuid(@Nonnull Player player, @Nonnull Vec3 pos, double epsilon) {
        ListTag list = getPointList(player);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            Vec3 stored = new Vec3(tag.getDouble(X_KEY), tag.getDouble(Y_KEY), tag.getDouble(Z_KEY));
            if (stored.distanceToSqr(pos) < epsilon) {
                return tag.getString(ID_KEY);
            }
        }
        return null;
    }

    public static void removePointByUuid(@Nonnull Player player, @Nonnull String uuid) {
        CompoundTag data = getData(player);
        ListTag list = data.getList(POINTS_KEY, Tag.TAG_COMPOUND);
        // Clean up connections from other points
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            List<String> conns = getConnectionsList(tag);
            if (conns.remove(uuid)) {
                tag.put(CONNECTIONS_KEY, stringsToListTag(conns));
            }
        }
        list.removeIf(t -> ((CompoundTag) t).getString(ID_KEY).equals(uuid));
        data.put(POINTS_KEY, list);
        setData(player, data);
    }
    public static void togglePoint(@Nonnull Player player, @Nonnull Vec3 targetPos) {
        CompoundTag data = getData(player);
        ListTag list = data.getList(POINTS_KEY, Tag.TAG_COMPOUND);

        String existingUuid = findPointUuid(player, targetPos);
        if (existingUuid != null) {
            // Clean up connections from other points, then remove
            for (int i = 0; i < list.size(); i++) {
                CompoundTag tag = list.getCompound(i);
                List<String> conns = getConnectionsList(tag);
                if (conns.remove(existingUuid)) {
                    tag.put(CONNECTIONS_KEY, stringsToListTag(conns));
                }
            }
            list.removeIf(t -> ((CompoundTag) t).getString(ID_KEY).equals(existingUuid));
            player.displayClientMessage(Component.translatable("message.simpleportals.point_removed"), true);
        } else {
            CompoundTag pTag = new CompoundTag();
            pTag.putString(ID_KEY, UUID.randomUUID().toString());
            pTag.putDouble(X_KEY, targetPos.x);
            pTag.putDouble(Y_KEY, targetPos.y);
            pTag.putDouble(Z_KEY, targetPos.z);
            pTag.put(CONNECTIONS_KEY, new ListTag());
            list.add(pTag);
            player.displayClientMessage(Component.translatable("message.simpleportals.point_created"), true);
        }

        data.put(POINTS_KEY, list);
        setData(player, data);
    }

    public static void addConnection(@Nonnull Player player, @Nonnull String uuidA, @Nonnull String uuidB) {
        CompoundTag data = getData(player);
        ListTag list = data.getList(POINTS_KEY, Tag.TAG_COMPOUND);

        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            String id = tag.getString(ID_KEY);
            if (id.equals(uuidA)) addToConnections(tag, uuidB);
            if (id.equals(uuidB)) addToConnections(tag, uuidA);
        }

        data.put(POINTS_KEY, list);
        setData(player, data);
    }

    public static void removeConnection(@Nonnull Player player, @Nonnull String uuidA, @Nonnull String uuidB) {
        CompoundTag data = getData(player);
        ListTag list = data.getList(POINTS_KEY, Tag.TAG_COMPOUND);

        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            String id = tag.getString(ID_KEY);
            if (id.equals(uuidA)) removeFromConnections(tag, uuidB);
            if (id.equals(uuidB)) removeFromConnections(tag, uuidA);
        }

        data.put(POINTS_KEY, list);
        setData(player, data);
    }

    public static boolean hasConnection(@Nonnull Player player, @Nonnull String uuidA, @Nonnull String uuidB) {
        CompoundTag tagA = getPointByUuid(player, uuidA);
        if (tagA == null) return false;
        return getConnectionsList(tagA).contains(uuidB);
    }

    @Nonnull
    public static List<String> getConnections(@Nonnull Player player, @Nonnull String uuid) {
        CompoundTag tag = getPointByUuid(player, uuid);
        if (tag == null) return new ArrayList<>();
        return getConnectionsList(tag);
    }

    public static Vec3 getPointPosByUuid(@Nonnull Player player, @Nonnull String uuid) {
        CompoundTag tag = getPointByUuid(player, uuid);
        if (tag == null) return null;
        return new Vec3(tag.getDouble(X_KEY), tag.getDouble(Y_KEY), tag.getDouble(Z_KEY));
    }

    @Nonnull
    public static String getSelectedEndpoint(@Nonnull Player player) {
        CompoundTag data = getData(player);
        if (data.contains(SELECTED_KEY, Tag.TAG_STRING)) {
            return data.getString(SELECTED_KEY);
        }
        return "";
    }

    public static void setSelectedEndpoint(@Nonnull Player player, String uuid) {
        CompoundTag data = getData(player);
        if (uuid == null || uuid.isEmpty()) {
            data.remove(SELECTED_KEY);
        } else {
            data.putString(SELECTED_KEY, uuid);
        }
        setData(player, data);
    }

    private static CompoundTag findTagByUuid(@Nonnull ListTag list, @Nonnull String uuid) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            if (uuid.equals(tag.getString(ID_KEY))) return tag;
        }
        return null;
    }

    @Nonnull
    private static List<String> getConnectionsList(@Nonnull CompoundTag tag) {
        List<String> result = new ArrayList<>();
        ListTag list = tag.getList(CONNECTIONS_KEY, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            result.add(list.getString(i));
        }
        return result;
    }

    private static void addToConnections(@Nonnull CompoundTag tag, @Nonnull String uuid) {
        ListTag list = tag.getList(CONNECTIONS_KEY, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            if (uuid.equals(list.getString(i))) return; // no dupes
        }
        list.add(StringTag.valueOf(uuid));
        tag.put(CONNECTIONS_KEY, list);
    }

    private static void removeFromConnections(@Nonnull CompoundTag tag, @Nonnull String uuid) {
        ListTag list = tag.getList(CONNECTIONS_KEY, Tag.TAG_STRING);
        list.removeIf(t -> ((StringTag) t).getAsString().equals(uuid));
        tag.put(CONNECTIONS_KEY, list);
    }

    @Nonnull
    private static ListTag stringsToListTag(@Nonnull List<String> strings) {
        ListTag list = new ListTag();
        for (String s : strings) {
            list.add(StringTag.valueOf(s));
        }
        return list;
    }

    // ─── Surface storage ───

    public static void addSurface(@Nonnull Player player, @Nonnull List<String> orderedUuids) {
        CompoundTag data = getData(player);
        ListTag surfaces = data.getList(SURFACES_KEY, Tag.TAG_COMPOUND);
        CompoundTag surf = new CompoundTag();
        surf.putString(SURFACE_ID_KEY, UUID.randomUUID().toString());
        ListTag pointIds = new ListTag();
        for (String uuid : orderedUuids) {
            pointIds.add(StringTag.valueOf(uuid));
        }
        surf.put(SURFACE_POINTS_KEY, pointIds);
        surfaces.add(surf);
        data.put(SURFACES_KEY, surfaces);
        setData(player, data);
    }

    public static void removeSurface(@Nonnull Player player, @Nonnull String surfaceId) {
        CompoundTag data = getData(player);
        ListTag surfaces = data.getList(SURFACES_KEY, Tag.TAG_COMPOUND);
        surfaces.removeIf(t -> ((CompoundTag)t).getString(SURFACE_ID_KEY).equals(surfaceId));
        data.put(SURFACES_KEY, surfaces);
        setData(player, data);
    }

    @Nonnull
    public static ListTag getSurfaces(@Nonnull Player player) {
        return getData(player).getList(SURFACES_KEY, Tag.TAG_COMPOUND);
    }

    /** Find the surface that contains both given point UUIDs as consecutive or wrap-around neighbors. */
    @Nullable
    public static String findSurfaceByEdge(@Nonnull Player player, @Nonnull String uuidA, @Nonnull String uuidB) {
        ListTag surfaces = getSurfaces(player);
        for (int i = 0; i < surfaces.size(); i++) {
            CompoundTag surf = surfaces.getCompound(i);
            ListTag pts = surf.getList(SURFACE_POINTS_KEY, Tag.TAG_STRING);
            if (hasEdge(pts, uuidA, uuidB)) {
                return surf.getString(SURFACE_ID_KEY);
            }
        }
        return null;
    }

    private static boolean hasEdge(ListTag pts, String a, String b) {
        int n = pts.size();
        if (n < 2) return false;
        for (int i = 0; i < n; i++) {
            String cur = pts.getString(i);
            String next = pts.getString((i + 1) % n);
            if ((cur.equals(a) && next.equals(b)) || (cur.equals(b) && next.equals(a))) return true;
        }
        return false;
    }

    /** Get the ordered Vec3 positions for all points in a surface. */
    @Nonnull
    public static List<Vec3> getSurfacePositions(@Nonnull Player player, @Nonnull CompoundTag surf) {
        List<Vec3> positions = new ArrayList<>();
        ListTag pts = surf.getList(SURFACE_POINTS_KEY, Tag.TAG_STRING);
        for (int i = 0; i < pts.size(); i++) {
            Vec3 p = getPointPosByUuid(player, pts.getString(i));
            if (p != null) positions.add(p);
        }
        return positions;
    }

    // ─── Cycle detection ───

    /**
     * Find the smallest simple cycle (closed loop) that contains the given point UUID.
     * Returns ordered list of point UUIDs forming the cycle, or null if none found.
     */
    @Nullable
    public static List<String> findSmallestCycleContaining(@Nonnull Player player, @Nonnull String pointUuid) {
        ListTag pointList = getPointList(player);
        java.util.Map<String, java.util.List<String>> graph = new java.util.HashMap<>();
        for (int i = 0; i < pointList.size(); i++) {
            CompoundTag tag = pointList.getCompound(i);
            String id = tag.getString(ID_KEY);
            graph.put(id, getConnectionsList(tag));
        }

        List<String> bestCycle = null;
        java.util.Set<String> visited = new java.util.HashSet<>();
        java.util.List<String> path = new java.util.ArrayList<>();
        path.add(pointUuid);
        visited.add(pointUuid);
        bestCycle = dfsFindCycle(graph, pointUuid, pointUuid, visited, path, bestCycle);
        return bestCycle;
    }

    @Nullable
    private static List<String> dfsFindCycle(java.util.Map<String, java.util.List<String>> graph,
                                              String start, String current, java.util.Set<String> visited,
                                              java.util.List<String> path, List<String> bestCycle) {
        for (String neighbor : graph.getOrDefault(current, java.util.Collections.emptyList())) {
            // skip going back to the node we just came from
            if (path.size() >= 2 && neighbor.equals(path.get(path.size() - 2))) continue;

            if (neighbor.equals(start) && path.size() >= 3) {
                // found a cycle back to start
                if (bestCycle == null || path.size() < bestCycle.size()) {
                    bestCycle = new ArrayList<>(path);
                }
                continue;
            }

            if (!visited.contains(neighbor)) {
                visited.add(neighbor);
                path.add(neighbor);
                bestCycle = dfsFindCycle(graph, start, neighbor, visited, path, bestCycle);
                path.remove(path.size() - 1);
                visited.remove(neighbor);
            }
        }
        return bestCycle;
    }

    /** Get Vec3 positions for a list of point UUIDs in order. */
    @Nonnull
    public static List<Vec3> getPositionsByUuids(@Nonnull Player player, @Nonnull List<String> uuids) {
        List<Vec3> positions = new ArrayList<>();
        for (String uuid : uuids) {
            Vec3 p = getPointPosByUuid(player, uuid);
            if (p != null) positions.add(p);
        }
        return positions;
    }
}
