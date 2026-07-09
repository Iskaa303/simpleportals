package net.iskaa303.simpleportals.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;

/**
 * Renders the snap grid overlay on the nearest surface (or facing plane in mid-air).
 */
public final class GridRenderer {

    private GridRenderer() {}

    public static void render(@Nonnull PoseStack poseStack, @Nonnull Vec3 camPos, Vec3 lookVec,
                              @Nonnull BlockHitResult hitResult, @Nonnull Vec3 renderPos, VertexConsumer builder) {
        Direction normal = hitResult.getType() != HitResult.Type.MISS
                ? hitResult.getDirection()
                : Direction.getNearest(lookVec.x, lookVec.y, lookVec.z);

        float playerDistanceFade = RenderUtils.getFade(
                camPos.distanceTo(renderPos),
                RenderConstants.PLAYER_VISIBLE_DISTANCE,
                RenderConstants.PLAYER_FADE_DISTANCE);
        if (playerDistanceFade <= 0) return;

        Vec3i normalVecInt = normal.getNormal();
        if (normalVecInt == null) return;
        Vec3 normalVec = Vec3.atLowerCornerOf(normalVecInt).scale(0.003);

        Vec3 anchor = hitResult.getType() != HitResult.Type.MISS
                ? hitResult.getLocation().add(normalVec)
                : renderPos;

        double centerA = getPlaneAxis(renderPos, normal, true);
        double centerB = getPlaneAxis(renderPos, normal, false);

        double startA = RenderUtils.snapToGrid(centerA - RenderConstants.GRID_RENDER_RADIUS);
        double startB = RenderUtils.snapToGrid(centerB - RenderConstants.GRID_RENDER_RADIUS);

        for (double a = startA; a <= centerA + RenderConstants.GRID_RENDER_RADIUS; a += RenderConstants.BOX_SIZE) {
            renderGridLine(poseStack, builder, normal, anchor, a,
                    centerB - RenderConstants.GRID_RENDER_RADIUS, centerB + RenderConstants.GRID_RENDER_RADIUS,
                    centerA, centerB, camPos, true, playerDistanceFade);
        }
        for (double b = startB; b <= centerB + RenderConstants.GRID_RENDER_RADIUS; b += RenderConstants.BOX_SIZE) {
            renderGridLine(poseStack, builder, normal, anchor, b,
                    centerA - RenderConstants.GRID_RENDER_RADIUS, centerA + RenderConstants.GRID_RENDER_RADIUS,
                    centerB, centerA, camPos, false, playerDistanceFade);
        }
    }

    private static void renderGridLine(PoseStack ps, VertexConsumer b, Direction n, Vec3 anc,
                                       double fix, double minV, double maxV,
                                       double fixC, double varC, @Nonnull Vec3 cam,
                                       boolean isFixA, float playerFade) {
        double gridCoord = Math.abs(fix);
        boolean isBlockBoundary = Math.abs(gridCoord - Math.round(gridCoord)) < 0.01;

        float baseAlpha = isBlockBoundary ? 1.00f : 0.62f;
        double thicknessScale = isBlockBoundary ? 3.0 : 1.0;

        for (double v = minV; v < maxV; v += RenderConstants.BOX_SIZE) {
            double nextV = v + RenderConstants.BOX_SIZE;
            double dist = Math.sqrt(
                    Math.pow(fix - fixC, 2) +
                    Math.pow(((v + nextV) * 0.5) - varC, 2));

            float alpha = baseAlpha *
                    RenderUtils.getFade(dist, RenderConstants.GRID_RENDER_RADIUS, RenderConstants.GRID_FADE_DISTANCE) *
                    playerFade;
            if (alpha <= 0) continue;

            Vec3 start = createPoint(n, anc, isFixA ? fix : v, isFixA ? v : fix);
            Vec3 end = createPoint(n, anc, isFixA ? fix : nextV, isFixA ? nextV : fix);
            Vec3i normalVecInt = n.getNormal();
            if (normalVecInt == null) continue;

            RenderUtils.renderLine(ps, b, start, end, alpha,
                    Vec3.atLowerCornerOf(normalVecInt), cam, thicknessScale);
        }
    }

    static Vec3 createPoint(Direction n, Vec3 anc, double a, double b) {
        return switch (n.getAxis()) {
            case X -> new Vec3(anc.x, a, b);
            case Y -> new Vec3(a, anc.y, b);
            case Z -> new Vec3(a, b, anc.z);
        };
    }

    private static double getPlaneAxis(Vec3 p, Direction n, boolean isA) {
        return switch (n.getAxis()) {
            case X -> isA ? p.y : p.z;
            case Y -> isA ? p.x : p.z;
            case Z -> isA ? p.x : p.y;
        };
    }
}
