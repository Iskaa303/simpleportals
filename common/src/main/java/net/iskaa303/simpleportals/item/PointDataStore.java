package net.iskaa303.simpleportals.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player points + connections.
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

    private static final double EPSILON = 0.0001;

    private static final Map<UUID, CompoundTag> PLAYER_DATA = new ConcurrentHashMap<>();

    private PointDataStore() {}

    private static CompoundTag getData(@Nonnull Player player) {
        return PLAYER_DATA.computeIfAbsent(player.getUUID(), k -> new CompoundTag());
    }

    private static void setData(@Nonnull Player player, @Nonnull CompoundTag data) {
        PLAYER_DATA.put(player.getUUID(), data);
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
            player.displayClientMessage(Component.literal("§cPoint Removed"), true);
        } else {
            CompoundTag pTag = new CompoundTag();
            pTag.putString(ID_KEY, UUID.randomUUID().toString());
            pTag.putDouble(X_KEY, targetPos.x);
            pTag.putDouble(Y_KEY, targetPos.y);
            pTag.putDouble(Z_KEY, targetPos.z);
            pTag.put(CONNECTIONS_KEY, new ListTag());
            list.add(pTag);
            player.displayClientMessage(Component.literal("§bPoint Created"), true);
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
}
