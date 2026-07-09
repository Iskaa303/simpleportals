package net.iskaa303.simpleportals.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;

/**
 * Shared rendering utilities: line drawing, distance fade, grid snapping.
 */
public final class RenderUtils {

    private RenderUtils() {}

    /**
     * Render a thick line from {@code s} to {@code e} with distance-based thickness.
     */
    public static void renderLine(@Nonnull PoseStack ps, VertexConsumer b, Vec3 s, Vec3 e,
                                  float alpha, Vec3 normal, @Nonnull Vec3 cam) {
        renderLine(ps, b, s, e, alpha, normal, cam, 1.0);
    }

    /**
     * Render a thick line with a custom thickness scale factor.
     */
    public static void renderLine(@Nonnull PoseStack ps, VertexConsumer b, Vec3 s, Vec3 e,
                                  float alpha, Vec3 normal, @Nonnull Vec3 cam, double thicknessScale) {
        if (s == null || e == null) return;

        Vec3 dir = e.subtract(s).normalize();
        Vec3 view = s.add(e).scale(0.5).subtract(cam).normalize();
        if (view == null) return;

        double thickness = RenderConstants.GRID_LINE_THICKNESS * thicknessScale;
        Vec3 offset = dir.cross(view).normalize().scale(thickness * (s.distanceTo(cam) * 0.25 + 1.0));

        PoseStack.Pose last = ps.last();
        if (last == null) return;

        b.addVertex(last, (float) (s.x - offset.x), (float) (s.y - offset.y), (float) (s.z - offset.z))
                .setColor(0.94f, 0.98f, 1.0f, alpha);
        b.addVertex(last, (float) (e.x - offset.x), (float) (e.y - offset.y), (float) (e.z - offset.z))
                .setColor(0.94f, 0.98f, 1.0f, alpha);
        b.addVertex(last, (float) (e.x + offset.x), (float) (e.y + offset.y), (float) (e.z + offset.z))
                .setColor(0.94f, 0.98f, 1.0f, alpha);
        b.addVertex(last, (float) (s.x + offset.x), (float) (s.y + offset.y), (float) (s.z + offset.z))
                .setColor(0.94f, 0.98f, 1.0f, alpha);
    }

    /**
     * Smooth Hermite-style fade: 1.0 within {@code rad - fade}, 0.0 at {@code rad}.
     */
    public static float getFade(double dist, double rad, double fade) {
        double start = rad - fade;
        if (dist <= start) return 1.0f;
        if (dist >= rad) return 0.0f;
        double p = (dist - start) / fade;
        return (float) (1.0 - (p * p * (3.0 - 2.0 * p)));
    }

    /** Snap a scalar value to the nearest grid cell (floor-based). */
    public static double snapToGrid(double val) {
        return Mth.floor(val / RenderConstants.BOX_SIZE) * RenderConstants.BOX_SIZE;
    }
}
