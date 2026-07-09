package net.iskaa303.simpleportals.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.iskaa303.simpleportals.item.PointDataStore;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;

/**
 * Renders points (blue/cyan/yellow), connection lines (cyan).
 * Colours: no connections = blue, has connections = cyan, selected endpoint = yellow.
 */
public final class SavedPointsRenderer {

    private static final float[] POINT_NORMAL  = {0.6f, 0.8f, 1.0f, 0.5f};  // blue
    private static final float[] POINT_CONN    = {0.0f, 1.0f, 1.0f, 0.6f};  // cyan
    private static final float[] POINT_SELECTED = {1.0f, 1.0f, 0.0f, 0.7f}; // yellow
    private static final float CONN_ALPHA = 0.5f;

    private SavedPointsRenderer() {}

    public static void render(@Nonnull PoseStack ps, @Nonnull Player player,
                              VertexConsumer builder, @Nonnull Vec3 camPos) {
        ListTag pointList = PointDataStore.getPointList(player);
        if (pointList.isEmpty()) return;

        float h = (float) (RenderConstants.BOX_SIZE / 2.0);
        PoseStack.Pose last = ps.last();
        if (last == null) return;

        String selectedUuid = PointDataStore.getSelectedEndpoint(player);

        renderConnections(ps, builder, pointList, camPos);

        for (int i = 0; i < pointList.size(); i++) {
            CompoundTag tag = pointList.getCompound(i);
            Vec3 center = new Vec3(tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z"));
            String uuid = tag.getString("id");
            ListTag conns = tag.getList("connections", Tag.TAG_STRING);

            float[] color;
            if (selectedUuid != null && !selectedUuid.isEmpty() && selectedUuid.equals(uuid)) {
                color = POINT_SELECTED;
            } else if (!conns.isEmpty()) {
                color = POINT_CONN;
            } else {
                color = POINT_NORMAL;
            }

            for (Direction dir : Direction.values()) {
                renderFace(last, builder, center, h, dir, color);
            }
        }
    }

    private static void renderConnections(@Nonnull PoseStack ps, VertexConsumer b, @Nonnull ListTag points, @Nonnull Vec3 camPos) {
        for (int i = 0; i < points.size(); i++) {
            CompoundTag tagA = points.getCompound(i);
            Vec3 a = new Vec3(tagA.getDouble("x"), tagA.getDouble("y"), tagA.getDouble("z"));
            String uuidA = tagA.getString("id");

            ListTag conns = tagA.getList("connections", Tag.TAG_STRING);
            for (int j = 0; j < conns.size(); j++) {
                String uuidB = conns.getString(j);
                if (uuidA.compareTo(uuidB) >= 0) continue; // each pair once

                CompoundTag tagB = findTagByUuid(points, uuidB);
                if (tagB == null) continue;
                Vec3 b2 = new Vec3(tagB.getDouble("x"), tagB.getDouble("y"), tagB.getDouble("z"));

                RenderUtils.renderLine(ps, b, a, b2, CONN_ALPHA,
                        a.subtract(b2).normalize(), camPos, 2.0);
            }
        }
    }

    private static CompoundTag findTagByUuid(@Nonnull ListTag list, @Nonnull String uuid) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            if (uuid.equals(tag.getString("id"))) return tag;
        }
        return null;
    }

    private static void renderFace(@Nonnull PoseStack.Pose last, VertexConsumer b,
                                   Vec3 center, float h, Direction dir, float[] color) {
        Vec3i normalVecInt = dir.getNormal();
        if (normalVecInt == null) return;
        Vec3 n = Vec3.atLowerCornerOf(normalVecInt);

        Vec3 tangent = (Math.abs(n.y) > 0.5) ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = n.cross(tangent).normalize().scale(h);
        if (u == null) return;
        Vec3 v = n.cross(u).normalize().scale(h);
        if (v == null) return;

        Vec3 scaledN = n.scale(h + 0.001f);
        if (scaledN == null) return;
        Vec3 fC = center.add(scaledN);

        Vec3 p1 = fC.subtract(u).subtract(v);
        Vec3 p2 = fC.add(u).subtract(v);
        Vec3 p3 = fC.add(u).add(v);
        Vec3 p4 = fC.subtract(u).add(v);

        b.addVertex(last, (float) p1.x, (float) p1.y, (float) p1.z).setColor(color[0], color[1], color[2], color[3]);
        b.addVertex(last, (float) p2.x, (float) p2.y, (float) p2.z).setColor(color[0], color[1], color[2], color[3]);
        b.addVertex(last, (float) p3.x, (float) p3.y, (float) p3.z).setColor(color[0], color[1], color[2], color[3]);
        b.addVertex(last, (float) p4.x, (float) p4.y, (float) p4.z).setColor(color[0], color[1], color[2], color[3]);
    }
}
