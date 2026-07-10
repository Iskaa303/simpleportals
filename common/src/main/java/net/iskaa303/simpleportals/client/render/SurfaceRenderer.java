package net.iskaa303.simpleportals.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.iskaa303.simpleportals.client.gui.DragController;
import net.iskaa303.simpleportals.client.targeting.TargetSelector;
import net.iskaa303.simpleportals.item.PointDataStore;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import java.util.List;

/** Renders saved surfaces and the preview surface as semi-transparent polygons. */
public final class SurfaceRenderer {

    // Preview (would-be) surface: lighter, more transparent
    private static final float[] PREVIEW_FILL = {0.9f, 0.4f, 0.8f, 0.18f};
    private static final float[] PREVIEW_OUTLINE = {1.0f, 0.6f, 0.9f, 0.4f};

    private SurfaceRenderer() {}

    public static void render(@Nonnull PoseStack ps, @Nonnull Player player,
                              VertexConsumer builder, @Nonnull Vec3 camPos) {
        // Render saved surfaces with per-surface colors
        ListTag surfaces = PointDataStore.getSurfaces(player);
        for (int i = 0; i < surfaces.size(); i++) {
            CompoundTag surf = surfaces.getCompound(i);
            String surfId = surf.getString("surface_id");
            List<Vec3> verts = getSurfacePositionsWithDrag(player, surf);
            if (verts.size() < 3) continue;
            float[] fill, outline;
            if (DragController.isSurfaceAffected(surfId)) {
                fill = DragController.DRAG_SURFACE_FILL;
                outline = DragController.DRAG_SURFACE_OUTLINE;
            } else {
                fill = PointDataStore.getSurfaceFillColor(surf);
                outline = PointDataStore.getSurfaceOutlineColor(surf);
            }
            renderPolygon(ps, builder, verts, fill, outline, camPos);
        }

        // Render preview surface (cursor on point with cycle)
        List<Vec3> previewVerts = TargetSelector.getPreviewSurfaceVertices();
        if (previewVerts != null && previewVerts.size() >= 3) {
            renderPolygon(ps, builder, previewVerts, PREVIEW_FILL, PREVIEW_OUTLINE, camPos);
        }
    }

    /** Get surface vertex positions, applying drag offsets. */
    private static List<Vec3> getSurfacePositionsWithDrag(@Nonnull Player player, @Nonnull CompoundTag surf) {
        List<Vec3> positions = new java.util.ArrayList<>();
        ListTag pts = surf.getList("points", Tag.TAG_STRING);
        for (int i = 0; i < pts.size(); i++) {
            String uuid = pts.getString(i);
            Vec3 stored = PointDataStore.getPointPosByUuid(player, uuid);
            if (stored != null) {
                positions.add(DragController.getDisplayPosition(stored, uuid));
            }
        }
        return positions;
    }

    /**
     * Render a filled polygon (fan from first vertex) with wireframe outline.
     */
    private static void renderPolygon(@Nonnull PoseStack ps, VertexConsumer b,
                                      @Nonnull List<Vec3> verts,
                                      float[] fillColor, float[] outlineColor,
                                      @Nonnull Vec3 camPos) {
        PoseStack.Pose last = ps.last();
        if (last == null) return;

        int n = verts.size();
        if (n < 3) return;

        Vec3 v0 = verts.get(0);
        for (int i = 1; i < n - 1; i++) {
            Vec3 v1 = verts.get(i);
            Vec3 v2 = verts.get(i + 1);

            // Front face: v0,v1,v2
            b.addVertex(last, (float) v0.x, (float) v0.y, (float) v0.z)
                    .setColor(fillColor[0], fillColor[1], fillColor[2], fillColor[3]);
            b.addVertex(last, (float) v1.x, (float) v1.y, (float) v1.z)
                    .setColor(fillColor[0], fillColor[1], fillColor[2], fillColor[3]);
            b.addVertex(last, (float) v2.x, (float) v2.y, (float) v2.z)
                    .setColor(fillColor[0], fillColor[1], fillColor[2], fillColor[3]);
            b.addVertex(last, (float) v2.x, (float) v2.y, (float) v2.z)
                    .setColor(fillColor[0], fillColor[1], fillColor[2], fillColor[3]);
            // Back face: v2,v1,v0 (reversed winding)
            b.addVertex(last, (float) v2.x, (float) v2.y, (float) v2.z)
                    .setColor(fillColor[0], fillColor[1], fillColor[2], fillColor[3]);
            b.addVertex(last, (float) v1.x, (float) v1.y, (float) v1.z)
                    .setColor(fillColor[0], fillColor[1], fillColor[2], fillColor[3]);
            b.addVertex(last, (float) v0.x, (float) v0.y, (float) v0.z)
                    .setColor(fillColor[0], fillColor[1], fillColor[2], fillColor[3]);
            b.addVertex(last, (float) v0.x, (float) v0.y, (float) v0.z)
                    .setColor(fillColor[0], fillColor[1], fillColor[2], fillColor[3]);
        }

        // Wireframe outline
        for (int i = 0; i < n; i++) {
            Vec3 a = verts.get(i);
            Vec3 b2 = verts.get((i + 1) % n);
            Vec3 normal = a.subtract(b2).normalize();
            RenderUtils.renderLine(ps, b, a, b2, outlineColor[3], normal, camPos, 1.5);
        }
    }
}
