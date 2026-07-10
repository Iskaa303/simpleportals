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
import java.util.Random;
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
    private static final String COLOR_R_KEY = "color_r";
    private static final String COLOR_G_KEY = "color_g";
    private static final String COLOR_B_KEY = "color_b";
    private static final String COLOR_A_KEY = "color_a";
    private static final String CONNECTED_SURFACES_KEY = "connected_surfaces";
    private static final double EPSILON = 0.0001;
    private static final String MODE_KEY = "portal_stick_mode";

    private static final Map<UUID, CompoundTag> PLAYER_DATA = new ConcurrentHashMap<>();

    // ─── Color palette ───
    private static final float[][] SURFACE_COLORS = {
        {0.8f, 0.2f, 0.7f, 0.30f},  // magenta
        {0.2f, 0.6f, 1.0f, 0.30f},  // blue
        {0.2f, 1.0f, 0.5f, 0.30f},  // green
        {1.0f, 0.8f, 0.2f, 0.30f},  // yellow
        {1.0f, 0.4f, 0.2f, 0.30f},  // orange
        {0.6f, 0.2f, 1.0f, 0.30f},  // purple
        {1.0f, 0.2f, 0.2f, 0.30f},  // red
        {0.2f, 1.0f, 1.0f, 0.30f},  // cyan
        {1.0f, 0.6f, 0.8f, 0.30f},  // pink
        {0.6f, 1.0f, 0.2f, 0.30f},  // lime
        {0.4f, 0.2f, 0.6f, 0.30f},  // indigo
        {1.0f, 0.8f, 0.6f, 0.30f},  // peach
    };
    private static final float[][] SURFACE_OUTLINES = {
        {1.0f, 0.4f, 0.9f, 0.6f},   // magenta
        {0.4f, 0.8f, 1.0f, 0.6f},   // blue
        {0.4f, 1.0f, 0.7f, 0.6f},   // green
        {1.0f, 0.9f, 0.4f, 0.6f},   // yellow
        {1.0f, 0.6f, 0.4f, 0.6f},   // orange
        {0.8f, 0.4f, 1.0f, 0.6f},   // purple
        {1.0f, 0.4f, 0.4f, 0.6f},   // red
        {0.4f, 1.0f, 1.0f, 0.6f},   // cyan
        {1.0f, 0.7f, 0.9f, 0.6f},   // pink
        {0.8f, 1.0f, 0.4f, 0.6f},   // lime
        {0.6f, 0.4f, 0.8f, 0.6f},   // indigo
        {1.0f, 0.9f, 0.7f, 0.6f},   // peach
    };
    private static final Random COLOR_RNG = new Random();

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

    public static void setPointPos(@Nonnull Player player, @Nonnull String uuid, @Nonnull Vec3 pos) {
        CompoundTag tag = getPointByUuid(player, uuid);
        if (tag == null) return;
        tag.putDouble(X_KEY, pos.x);
        tag.putDouble(Y_KEY, pos.y);
        tag.putDouble(Z_KEY, pos.z);
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

    /** Pick a random color index and assign to a new surface. */
    private static int pickColorIndex() {
        return COLOR_RNG.nextInt(SURFACE_COLORS.length);
    }

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
        int c = pickColorIndex();
        surf.putFloat(COLOR_R_KEY, SURFACE_COLORS[c][0]);
        surf.putFloat(COLOR_G_KEY, SURFACE_COLORS[c][1]);
        surf.putFloat(COLOR_B_KEY, SURFACE_COLORS[c][2]);
        surf.putFloat(COLOR_A_KEY, SURFACE_COLORS[c][3]);
        surf.put(CONNECTED_SURFACES_KEY, new ListTag());
        surfaces.add(surf);
        data.put(SURFACES_KEY, surfaces);
        setData(player, data);
    }

    public static void removeSurface(@Nonnull Player player, @Nonnull String surfaceId) {
        CompoundTag data = getData(player);
        // Remove this surface from any connected surfaces' lists
        ListTag surfaces = data.getList(SURFACES_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < surfaces.size(); i++) {
            CompoundTag s = surfaces.getCompound(i);
            List<String> conns = getConnectedSurfacesList(s);
            if (conns.remove(surfaceId)) {
                s.put(CONNECTED_SURFACES_KEY, stringsToListTag(conns));
            }
        }
        surfaces.removeIf(t -> ((CompoundTag)t).getString(SURFACE_ID_KEY).equals(surfaceId));
        data.put(SURFACES_KEY, surfaces);
        setData(player, data);
    }

    @Nonnull
    public static ListTag getSurfaces(@Nonnull Player player) {
        return getData(player).getList(SURFACES_KEY, Tag.TAG_COMPOUND);
    }

    /** Get the fill color for a surface tag. Returns [r,g,b,a]. */
    @Nonnull
    public static float[] getSurfaceFillColor(@Nonnull CompoundTag surf) {
        return new float[]{
            surf.getFloat(COLOR_R_KEY),
            surf.getFloat(COLOR_G_KEY),
            surf.getFloat(COLOR_B_KEY),
            surf.getFloat(COLOR_A_KEY)
        };
    }

    /** Get the outline color for a surface tag (brighter version of fill). */
    @Nonnull
    public static float[] getSurfaceOutlineColor(@Nonnull CompoundTag surf) {
        float[] fill = getSurfaceFillColor(surf);
        return new float[]{
            Math.min(1f, fill[0] + 0.2f),
            Math.min(1f, fill[1] + 0.2f),
            Math.min(1f, fill[2] + 0.2f),
            Math.min(1f, fill[3] + 0.3f)
        };
    }

    /** Assign the same color to two surfaces (for connecting). */
    public static void assignSameColor(@Nonnull Player player, @Nonnull String surfIdA, @Nonnull String surfIdB) {
        ListTag surfaces = getSurfaces(player);
        float[] colorSrc = null;
        for (int i = 0; i < surfaces.size(); i++) {
            CompoundTag s = surfaces.getCompound(i);
            if (s.getString(SURFACE_ID_KEY).equals(surfIdA)) {
                colorSrc = getSurfaceFillColor(s);
                break;
            }
        }
        if (colorSrc == null) return;
        for (int i = 0; i < surfaces.size(); i++) {
            CompoundTag s = surfaces.getCompound(i);
            if (s.getString(SURFACE_ID_KEY).equals(surfIdB)) {
                s.putFloat(COLOR_R_KEY, colorSrc[0]);
                s.putFloat(COLOR_G_KEY, colorSrc[1]);
                s.putFloat(COLOR_B_KEY, colorSrc[2]);
                s.putFloat(COLOR_A_KEY, colorSrc[3]);
                break;
            }
        }
    }

    /** Connect two surfaces (bidirectional). */
    public static void connectSurfaces(@Nonnull Player player, @Nonnull String surfIdA, @Nonnull String surfIdB) {
        ListTag surfaces = getSurfaces(player);
        for (int i = 0; i < surfaces.size(); i++) {
            CompoundTag s = surfaces.getCompound(i);
            String id = s.getString(SURFACE_ID_KEY);
            ListTag conns = s.getList(CONNECTED_SURFACES_KEY, Tag.TAG_STRING);
            if (id.equals(surfIdA) && !containsString(conns, surfIdB)) {
                conns.add(StringTag.valueOf(surfIdB));
                s.put(CONNECTED_SURFACES_KEY, conns);
            }
            if (id.equals(surfIdB) && !containsString(conns, surfIdA)) {
                conns.add(StringTag.valueOf(surfIdA));
                s.put(CONNECTED_SURFACES_KEY, conns);
            }
        }
        assignSameColor(player, surfIdA, surfIdB);
    }

    /** Disconnect two surfaces. */
    public static void disconnectSurfaces(@Nonnull Player player, @Nonnull String surfIdA, @Nonnull String surfIdB) {
        ListTag surfaces = getSurfaces(player);
        for (int i = 0; i < surfaces.size(); i++) {
            CompoundTag s = surfaces.getCompound(i);
            String id = s.getString(SURFACE_ID_KEY);
            ListTag conns = s.getList(CONNECTED_SURFACES_KEY, Tag.TAG_STRING);
            if (id.equals(surfIdA) || id.equals(surfIdB)) {
                conns.removeIf(t -> ((StringTag)t).getAsString().equals(
                    id.equals(surfIdA) ? surfIdB : surfIdA
                ));
                s.put(CONNECTED_SURFACES_KEY, conns);
            }
        }
    }

    /** Check if two surfaces are connected. */
    public static boolean areSurfacesConnected(@Nonnull Player player, @Nonnull String surfIdA, @Nonnull String surfIdB) {
        ListTag surfaces = getSurfaces(player);
        for (int i = 0; i < surfaces.size(); i++) {
            CompoundTag s = surfaces.getCompound(i);
            if (s.getString(SURFACE_ID_KEY).equals(surfIdA)) {
                return containsString(s.getList(CONNECTED_SURFACES_KEY, Tag.TAG_STRING), surfIdB);
            }
        }
        return false;
    }

    /** Get list of connected surface UUIDs for a surface. */
    @Nonnull
    public static List<String> getConnectedSurfaceIds(@Nonnull Player player, @Nonnull String surfaceId) {
        ListTag surfaces = getSurfaces(player);
        for (int i = 0; i < surfaces.size(); i++) {
            CompoundTag s = surfaces.getCompound(i);
            if (s.getString(SURFACE_ID_KEY).equals(surfaceId)) {
                List<String> result = new ArrayList<>();
                ListTag conns = s.getList(CONNECTED_SURFACES_KEY, Tag.TAG_STRING);
                for (int j = 0; j < conns.size(); j++) {
                    result.add(conns.getString(j));
                }
                return result;
            }
        }
        return new ArrayList<>();
    }

    /** Check if two surfaces have the same point count (same shape prerequisite). */
    public static boolean hasSameShape(@Nonnull Player player, @Nonnull String surfIdA, @Nonnull String surfIdB) {
        ListTag surfaces = getSurfaces(player);
        List<Vec3> posA = null, posB = null;
        for (int i = 0; i < surfaces.size(); i++) {
            CompoundTag s = surfaces.getCompound(i);
            String id = s.getString(SURFACE_ID_KEY);
            List<Vec3> pos = getSurfacePositions(player, s);
            if (id.equals(surfIdA)) posA = pos;
            if (id.equals(surfIdB)) posB = pos;
        }
        if (posA == null || posB == null || posA.size() < 3 || posA.size() != posB.size()) return false;
        int n = posA.size();

        // Compute edge lengths
        double[] edgesA = new double[n];
        double[] edgesB = new double[n];
        for (int i = 0; i < n; i++) {
            edgesA[i] = posA.get(i).distanceTo(posA.get((i + 1) % n));
            edgesB[i] = posB.get(i).distanceTo(posB.get((i + 1) % n));
        }

        // Normalize by average edge length (handles scaling)
        double avgA = 0, avgB = 0;
        for (int i = 0; i < n; i++) { avgA += edgesA[i]; avgB += edgesB[i]; }
        avgA /= n; avgB /= n;
        if (avgA < 1e-10 || avgB < 1e-10) return false;
        for (int i = 0; i < n; i++) { edgesA[i] /= avgA; edgesB[i] /= avgB; }

        // Try all cyclic shifts + reversal to handle ordering differences
        double best = Double.MAX_VALUE;
        for (int shift = 0; shift < n; shift++) {
            double fwd = 0, rev = 0;
            for (int i = 0; i < n; i++) {
                double dF = edgesA[i] - edgesB[(i + shift) % n];
                fwd += dF * dF;
                double dR = edgesA[i] - edgesB[(n - 1 - i + shift) % n];
                rev += dR * dR;
            }
            best = Math.min(best, Math.min(fwd, rev));
        }

        return best < 0.1 * n; // 10% RMS tolerance per edge
    }

    /** Duplicate a surface: copy all its points, create new surface with same shape, return new surface ID. */
    @Nonnull
    public static String copySurface(@Nonnull Player player, @Nonnull String sourceSurfaceId) {
        CompoundTag data = getData(player);
        ListTag pointList = data.getList(POINTS_KEY, Tag.TAG_COMPOUND);
        ListTag surfaces = data.getList(SURFACES_KEY, Tag.TAG_COMPOUND);

        // Find source surface
        CompoundTag srcSurf = null;
        for (int i = 0; i < surfaces.size(); i++) {
            CompoundTag s = surfaces.getCompound(i);
            if (s.getString(SURFACE_ID_KEY).equals(sourceSurfaceId)) {
                srcSurf = s;
                break;
            }
        }
        if (srcSurf == null) return "";

        // Duplicate each point (new UUID, same position)
        ListTag srcPtIds = srcSurf.getList(SURFACE_POINTS_KEY, Tag.TAG_STRING);
        List<String> newPtIds = new ArrayList<>();
        for (int i = 0; i < srcPtIds.size(); i++) {
            String oldUuid = srcPtIds.getString(i);
            CompoundTag oldPt = getPointByUuid(player, oldUuid);
            if (oldPt == null) continue;
            String newUuid = UUID.randomUUID().toString();
            CompoundTag newPt = new CompoundTag();
            newPt.putString(ID_KEY, newUuid);
            newPt.putDouble(X_KEY, oldPt.getDouble(X_KEY));
            newPt.putDouble(Y_KEY, oldPt.getDouble(Y_KEY));
            newPt.putDouble(Z_KEY, oldPt.getDouble(Z_KEY));
            newPt.put(CONNECTIONS_KEY, new ListTag());
            pointList.add(newPt);
            newPtIds.add(newUuid);
        }

        // Create new surface
        CompoundTag newSurf = new CompoundTag();
        String newSurfId = UUID.randomUUID().toString();
        newSurf.putString(SURFACE_ID_KEY, newSurfId);
        ListTag newPtList = new ListTag();
        for (String id : newPtIds) {
            newPtList.add(StringTag.valueOf(id));
        }
        newSurf.put(SURFACE_POINTS_KEY, newPtList);
        // Copy color from source
        newSurf.putFloat(COLOR_R_KEY, srcSurf.getFloat(COLOR_R_KEY));
        newSurf.putFloat(COLOR_G_KEY, srcSurf.getFloat(COLOR_G_KEY));
        newSurf.putFloat(COLOR_B_KEY, srcSurf.getFloat(COLOR_B_KEY));
        newSurf.putFloat(COLOR_A_KEY, srcSurf.getFloat(COLOR_A_KEY));
        newSurf.put(CONNECTED_SURFACES_KEY, new ListTag());
        surfaces.add(newSurf);

        data.put(POINTS_KEY, pointList);
        data.put(SURFACES_KEY, surfaces);
        setData(player, data);

        // Auto-connect copy to original
        connectSurfaces(player, sourceSurfaceId, newSurfId);

        return newSurfId;
    }

    /** Update all point positions for a surface (used by scale/rotate). */
    public static void setSurfacePointPositions(@Nonnull Player player, @Nonnull String surfaceId, @Nonnull List<Vec3> newPositions) {
        ListTag surfaces = getSurfaces(player);
        for (int i = 0; i < surfaces.size(); i++) {
            CompoundTag s = surfaces.getCompound(i);
            if (!s.getString(SURFACE_ID_KEY).equals(surfaceId)) continue;
            ListTag ptUuids = s.getList(SURFACE_POINTS_KEY, Tag.TAG_STRING);
            int n = Math.min(ptUuids.size(), newPositions.size());
            for (int j = 0; j < n; j++) {
                setPointPos(player, ptUuids.getString(j), newPositions.get(j));
            }
            break;
        }
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
            if (path.size() >= 2 && neighbor.equals(path.get(path.size() - 2))) continue;

            if (neighbor.equals(start) && path.size() >= 3) {
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

    // ─── Helpers ───

    private static boolean containsString(ListTag list, String value) {
        for (int i = 0; i < list.size(); i++) {
            if (list.getString(i).equals(value)) return true;
        }
        return false;
    }

    private static List<String> getConnectedSurfacesList(CompoundTag surf) {
        List<String> result = new ArrayList<>();
        ListTag list = surf.getList(CONNECTED_SURFACES_KEY, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            result.add(list.getString(i));
        }
        return result;
    }
}
