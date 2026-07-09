package net.iskaa303.simpleportals.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.iskaa303.simpleportals.item.DebugStick;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Renders semi-transparent filled boxes for all saved points.
 */
public final class SavedPointsRenderer {

    private static final float RED = 0.6f;
    private static final float GREEN = 0.8f;
    private static final float BLUE = 1.0f;
    private static final float ALPHA = 0.5f;

    private SavedPointsRenderer() {}

    public static void render(@Nonnull PoseStack ps, ItemStack stickStack, VertexConsumer builder) {
        List<Vec3> points = DebugStick.getPoints(stickStack);
        if (points.isEmpty()) return;

        float h = (float) (RenderConstants.BOX_SIZE / 2.0);
        PoseStack.Pose last = ps.last();
        if (last == null) return;

        for (Vec3 center : points) {
            for (Direction dir : Direction.values()) {
                renderFace(last, builder, center, h, dir);
            }
        }
    }

    private static void renderFace(@Nonnull PoseStack.Pose last, VertexConsumer b,
                                   Vec3 center, float h, Direction dir) {
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

        b.addVertex(last, (float) p1.x, (float) p1.y, (float) p1.z).setColor(RED, GREEN, BLUE, ALPHA);
        b.addVertex(last, (float) p2.x, (float) p2.y, (float) p2.z).setColor(RED, GREEN, BLUE, ALPHA);
        b.addVertex(last, (float) p3.x, (float) p3.y, (float) p3.z).setColor(RED, GREEN, BLUE, ALPHA);
        b.addVertex(last, (float) p4.x, (float) p4.y, (float) p4.z).setColor(RED, GREEN, BLUE, ALPHA);
    }
}
