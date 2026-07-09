package net.iskaa303.simpleportals.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;

/**
 * Renders the wireframe box at the current cursor position.
 */
public final class CursorRenderer {

    private static final int[][] EDGES = {
            {0, 1}, {2, 3}, {4, 5}, {6, 7},
            {0, 2}, {1, 3}, {4, 6}, {5, 7},
            {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };

    private CursorRenderer() {}

    public static void render(@Nonnull PoseStack ps, @Nonnull Vec3 camPos, Vec3 pos, VertexConsumer builder) {
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
}
