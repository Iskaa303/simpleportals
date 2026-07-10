package net.iskaa303.simpleportals.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/** Renders the cursor wireframe box. Stretched prism when connection-snapped. Polygon highlight when surface-snapped. */
public final class CursorRenderer {

    private static final int[][] EDGES = {
            {0, 1}, {2, 3}, {4, 5}, {6, 7},
            {0, 2}, {1, 3}, {4, 6}, {5, 7},
            {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };

    private CursorRenderer() {}

    public static void render(@Nonnull PoseStack ps, @Nonnull Vec3 camPos, Vec3 pos,
                              VertexConsumer builder, @Nullable Vec3[] connectionEndpoints,
                              @Nullable List<Vec3> surfaceVertices) {
        if (surfaceVertices != null && surfaceVertices.size() >= 3) {
            renderSurfaceHighlight(ps, camPos, builder, surfaceVertices);
        } else if (connectionEndpoints != null && connectionEndpoints.length == 2) {
            renderStretchedBox(ps, camPos, builder, connectionEndpoints[0], connectionEndpoints[1]);
        } else {
            renderSingleBox(ps, camPos, pos, builder);
        }
    }

    private static void renderSingleBox(@Nonnull PoseStack ps, @Nonnull Vec3 camPos, Vec3 pos, VertexConsumer builder) {
        double h = RenderConstants.BOX_SIZE / 2.0;
        Vec3[] pts = {
                new Vec3(pos.x - h, pos.y - h, pos.z - h),
                new Vec3(pos.x + h, pos.y - h, pos.z - h),
                new Vec3(pos.x - h, pos.y + h, pos.z - h),
                new Vec3(pos.x + h, pos.y + h, pos.z - h),
                new Vec3(pos.x - h, pos.y - h, pos.z + h),
                new Vec3(pos.x + h, pos.y - h, pos.z + h),
                new Vec3(pos.x - h, pos.y + h, pos.z + h),
                new Vec3(pos.x + h, pos.y + h, pos.z + h)
        };

        for (int[] e : EDGES) {
            Vec3 normal = pts[e[0]].subtract(pos).normalize();
            RenderUtils.renderLine(ps, builder, pts[e[0]], pts[e[1]], 1.0f, normal, camPos);
        }
    }

    /** Stretched prism from A to B — the connection runs through its center. */
    private static void renderStretchedBox(@Nonnull PoseStack ps, @Nonnull Vec3 camPos,
                                           VertexConsumer builder, Vec3 a, Vec3 b) {
        Vec3 d = b.subtract(a).normalize();
        double h = RenderConstants.BOX_SIZE / 2.0;

        Vec3 up = (Math.abs(d.y) < 0.9) ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 u = d.cross(up).normalize().scale(h);
        if (u == null) return;
        Vec3 v = d.cross(u).normalize().scale(h);
        if (v == null) return;

        Vec3[] corners = {
                a.add(u).add(v),  a.add(u).subtract(v),
                a.subtract(u).add(v), a.subtract(u).subtract(v),
                b.add(u).add(v),  b.add(u).subtract(v),
                b.subtract(u).add(v), b.subtract(u).subtract(v)
        };

        int[][] stretchEdges = {
                {0,1},{2,3},{0,2},{1,3},  // cap A
                {4,5},{6,7},{4,6},{5,7},  // cap B
                {0,4},{1,5},{2,6},{3,7}   // connect A→B
        };

        Vec3 center = a.add(b).scale(0.5);
        for (int[] e : stretchEdges) {
            Vec3 normal = corners[e[0]].subtract(center).normalize();
            RenderUtils.renderLine(ps, builder, corners[e[0]], corners[e[1]], 0.8f, normal, camPos);
        }
    }

    /**
     * Render a wireframe highlight around the surface polygon, shaped like a Minecraft block highlight.
     * The polygon is offset slightly along its normal for the highlight look.
     */
    private static void renderSurfaceHighlight(@Nonnull PoseStack ps, @Nonnull Vec3 camPos,
                                               VertexConsumer builder, @Nonnull List<Vec3> verts) {
        int n = verts.size();
        if (n < 3) return;

        // Compute polygon normal from first three vertices
        Vec3 v0 = verts.get(0);
        Vec3 v1 = verts.get(1);
        Vec3 v2 = verts.get(2);
        Vec3 normal = v1.subtract(v0).cross(v2.subtract(v0)).normalize();
        if (normal == null) normal = new Vec3(0, 1, 0);

        // ponytail: single wireframe offset slightly above the surface, same visual style as the normal cursor
        double offset = 0.003;
        Vec3 shift = normal.scale(offset);
        for (int i = 0; i < n; i++) {
            Vec3 a = verts.get(i).add(shift);
            Vec3 b = verts.get((i + 1) % n).add(shift);
            Vec3 edgeNormal = a.subtract(b).normalize();
            RenderUtils.renderLine(ps, builder, a, b, 1.0f, edgeNormal, camPos, 2.0);
        }
    }
}
