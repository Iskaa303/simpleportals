package net.iskaa303.simpleportals.entity;

import net.iskaa303.simpleportals.item.PointDataStore;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A standalone portal entity — does NOT extend Minecraft's Entity class.
 * Stored in PortalWorldData (SavedData) on the server, rendered independently on the client.
 * Points for editing are generated from the vertex list on load.
 */
public final class PortalEntity {

    private static final String UUID_KEY = "uuid";
    private static final String VERTICES_KEY = "vertices";
    private static final String VX_KEY = "vx";
    private static final String VY_KEY = "vy";
    private static final String VZ_KEY = "vz";
    private static final String COLOR_R = "cr";
    private static final String COLOR_G = "cg";
    private static final String COLOR_B = "cb";

    private final UUID uuid;
    private final List<Vec3> vertices;
    private final float r, g, b;

    public PortalEntity(UUID uuid, List<Vec3> vertices, float r, float g, float b) {
        this.uuid = uuid;
        this.vertices = new ArrayList<>(vertices);
        this.r = r;
        this.g = g;
        this.b = b;
    }

    /** Create a new portal with a random UUID. */
    public static PortalEntity create(List<Vec3> vertices, float r, float g, float b) {
        return new PortalEntity(UUID.randomUUID(), vertices, r, g, b);
    }

    @Nonnull
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(UUID_KEY, uuid);
        ListTag verts = new ListTag();
        for (Vec3 v : vertices) {
            CompoundTag vt = new CompoundTag();
            vt.putDouble(VX_KEY, v.x);
            vt.putDouble(VY_KEY, v.y);
            vt.putDouble(VZ_KEY, v.z);
            verts.add(vt);
        }
        tag.put(VERTICES_KEY, verts);
        tag.putFloat(COLOR_R, r);
        tag.putFloat(COLOR_G, g);
        tag.putFloat(COLOR_B, b);
        return tag;
    }

    @Nonnull
    public static PortalEntity load(CompoundTag tag) {
        UUID uuid = tag.getUUID(UUID_KEY);
        ListTag verts = tag.getList(VERTICES_KEY, Tag.TAG_COMPOUND);
        List<Vec3> vertices = new ArrayList<>();
        for (int i = 0; i < verts.size(); i++) {
            CompoundTag vt = verts.getCompound(i);
            vertices.add(new Vec3(vt.getDouble(VX_KEY), vt.getDouble(VY_KEY), vt.getDouble(VZ_KEY)));
        }
        float r = tag.getFloat(COLOR_R);
        float g = tag.getFloat(COLOR_G);
        float b = tag.getFloat(COLOR_B);
        return new PortalEntity(uuid, vertices, r, g, b);
    }

    /** Generate editor points from vertices and populate PointDataStore. */
    public void populateEditorData(@Nonnull Player player) {
        int n = vertices.size();
        if (n < 2) return;
        String puuid = uuid.toString();
        List<String> uuids = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String ptUuid = java.util.UUID.randomUUID().toString();
            PointDataStore.addPointWithUuid(player, ptUuid, vertices.get(i));
            PortalWorldData.registerEditorPoint(ptUuid, puuid, i);
            uuids.add(ptUuid);
        }
        // Connect consecutive points to form the cycle
        for (int i = 0; i < n; i++) {
            String a = uuids.get(i);
            String b = uuids.get((i + 1) % n);
            if (!PointDataStore.hasConnection(player, a, b)) {
                PointDataStore.addConnection(player, a, b);
            }
        }
    }

    // ─── Getters ───

    public UUID getUuid() { return uuid; }
    public List<Vec3> getVertices() { return new ArrayList<>(vertices); }
    public float getR() { return r; }
    public float getG() { return g; }
    public float getB() { return b; }

    public Vec3 getCentroid() {
        double cx = 0, cy = 0, cz = 0;
        int n = vertices.size();
        if (n == 0) return Vec3.ZERO;
        for (Vec3 v : vertices) { cx += v.x; cy += v.y; cz += v.z; }
        return new Vec3(cx / n, cy / n, cz / n);
    }

    public boolean rayIntersects(Vec3 from, Vec3 to) {
        int n = vertices.size();
        if (n < 3) return false;
        Vec3 dir = to.subtract(from);
        double maxDist = dir.length();
        if (maxDist < 1e-10) return false;
        dir = dir.normalize();
        Vec3 v0 = vertices.get(0);
        for (int i = 1; i < n - 1; i++) {
            Vec3 v1 = vertices.get(i);
            Vec3 v2 = vertices.get(i + 1);
            Vec3 edge1 = v1.subtract(v0);
            Vec3 edge2 = v2.subtract(v0);
            Vec3 h = dir.cross(edge2);
            double det = edge1.dot(h);
            if (Math.abs(det) < 1e-10) continue;
            double invDet = 1.0 / det;
            Vec3 s = from.subtract(v0);
            double u = s.dot(h) * invDet;
            if (u < 0 || u > 1) continue;
            Vec3 q = s.cross(edge1);
            double v = dir.dot(q) * invDet;
            if (v < 0 || u + v > 1) continue;
            double t = edge2.dot(q) * invDet;
            if (t > 1e-10 && t < maxDist) return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PortalEntity that)) return false;
        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() { return uuid.hashCode(); }

    @Override
    public String toString() {
        return "PortalEntity{" + uuid + ", vertices=" + vertices.size() + "}";
    }
}
