package net.iskaa303.simpleportals.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import foundry.veil.api.client.render.rendertype.VeilRenderType;
import net.iskaa303.simpleportals.Constants;
import net.iskaa303.simpleportals.SimplePortalsMod;
import net.iskaa303.simpleportals.item.DebugStick;
import net.iskaa303.simpleportals.registry.SimplePortalsItems;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;

public class SelectionInterfaceRenderer {
    private static final double REACH = 10.0;
    private static final double BOX_SIZE = 0.25;
    private static final double GRID_RENDER_RADIUS = 3.0;
    private static final double GRID_FADE_DISTANCE = 1.0;
    private static final double SNAP_SMOOTHING = 0.38;
    private static final double GRID_LINE_THICKNESS = 0.002;
    private static final double PLAYER_VISIBLE_DISTANCE = 6.0;
    private static final double PLAYER_FADE_DISTANCE = 2.0;

    private static final ResourceLocation SELECTION_INTERFACE_RENDER_TYPE = SimplePortalsMod.path("selection_interface");

    private static Vec3 smoothedRenderPos;

    private SelectionInterfaceRenderer() {}

    public static void render(@Nonnull PoseStack poseStack, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || !isHoldingDebugStick(player)) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 eyePos = camera.getPosition();
        Vec3 lookVec = player.getViewVector(partialTicks);
        Vec3 lookVecScaled = lookVec.scale(REACH);
        if (lookVecScaled == null) return;
        Vec3 reachVec = eyePos.add(lookVecScaled);
        if (reachVec == null) return;

        ClientLevel level = mc.level;
        if (level == null) return;
        BlockHitResult hitResult = level.clip(new ClipContext(eyePos, reachVec, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        
        Vec3 targetPos = hitResult.getType() != HitResult.Type.MISS ? hitResult.getLocation() : reachVec;
        if (player.isShiftKeyDown()) {
            targetPos = hitResult.getType() != HitResult.Type.MISS ? snapToSurface(hitResult) : snapToGrid(targetPos);
        }
        if (targetPos == null) return;

        RenderType renderType = VeilRenderType.get(SELECTION_INTERFACE_RENDER_TYPE, "simpleportals:item/debug_stick");
        if (renderType == null) {
            Constants.LOG.error("Failed to get RenderType for selection interface", SELECTION_INTERFACE_RENDER_TYPE);
            return;
        }

        Vec3 renderPos = smoothRenderPos(targetPos, player.isShiftKeyDown());
        VertexConsumer builder = mc.renderBuffers().bufferSource().getBuffer(renderType);
        if (builder == null) return;

        poseStack.pushPose();
        poseStack.translate(-eyePos.x, -eyePos.y, -eyePos.z);
        
        renderGrid(poseStack, eyePos, lookVec, hitResult, targetPos, builder);
        renderBox(poseStack, eyePos, renderPos, builder);
        
        mc.renderBuffers().bufferSource().endBatch(renderType);
        poseStack.popPose();
    }

    // --- Logic ---

    private static boolean isHoldingDebugStick(LocalPlayer player) {
        DebugStick stick = SimplePortalsItems.DEBUG_STICK.get();
        return stick != null && (player.getMainHandItem().is(stick) || player.getOffhandItem().is(stick));
    }

    private static Vec3 snapToGrid(Vec3 pos) {
        return new Vec3(Mth.floor(pos.x / BOX_SIZE) * BOX_SIZE, Mth.floor(pos.y / BOX_SIZE) * BOX_SIZE, Mth.floor(pos.z / BOX_SIZE) * BOX_SIZE);
    }

    private static Vec3 snapToSurface(BlockHitResult hitResult) {
        Vec3 pos = hitResult.getLocation();
        Direction face = hitResult.getDirection();
        double half = BOX_SIZE / 2.0;

        double x = face.getAxis() == Direction.Axis.X ? pos.x + (face.getStepX() * half) : Math.round(pos.x / BOX_SIZE) * BOX_SIZE;
        double y = face.getAxis() == Direction.Axis.Y ? pos.y + (face.getStepY() * half) : Math.round(pos.y / BOX_SIZE) * BOX_SIZE;
        double z = face.getAxis() == Direction.Axis.Z ? pos.z + (face.getStepZ() * half) : Math.round(pos.z / BOX_SIZE) * BOX_SIZE;

        return new Vec3(x, y, z);
    }

    private static Vec3 smoothRenderPos(@Nonnull Vec3 targetPos, boolean shouldSmooth) {
        if (!shouldSmooth || smoothedRenderPos == null || smoothedRenderPos.distanceToSqr(targetPos) > 4.0) {
            smoothedRenderPos = targetPos;
            return targetPos;
        }
        smoothedRenderPos = smoothedRenderPos.lerp(targetPos, SNAP_SMOOTHING);
        return smoothedRenderPos;
    }

    // --- Grid Rendering ---

    private static void renderGrid(PoseStack poseStack, @Nonnull Vec3 camPos, Vec3 lookVec, BlockHitResult hitResult, @Nonnull Vec3 renderPos, VertexConsumer builder) {
        Direction normal = hitResult.getType() != HitResult.Type.MISS ? hitResult.getDirection() : Direction.getNearest(lookVec.x, lookVec.y, lookVec.z);
        
        float playerDistanceFade = getFade(camPos.distanceTo(renderPos), PLAYER_VISIBLE_DISTANCE, PLAYER_FADE_DISTANCE);
        if (playerDistanceFade <= 0) return;

        Vec3i normalVecInt = normal.getNormal();
        if (normalVecInt == null) return;
        Vec3 normalVec = Vec3.atLowerCornerOf(normalVecInt).scale(0.003);
        if (normalVec == null) return;
        Vec3 anchor = hitResult.getType() != HitResult.Type.MISS ? hitResult.getLocation().add(normalVec) : renderPos;
        
        double centerA = getPlaneAxis(renderPos, normal, true);
        double centerB = getPlaneAxis(renderPos, normal, false);

        double startA = snapToGrid(centerA - GRID_RENDER_RADIUS);
        double startB = snapToGrid(centerB - GRID_RENDER_RADIUS);

        for (double a = startA; a <= centerA + GRID_RENDER_RADIUS; a += BOX_SIZE) {
            renderGridLine(poseStack, builder, normal, anchor, a, centerB - GRID_RENDER_RADIUS, centerB + GRID_RENDER_RADIUS, centerA, centerB, camPos, true, playerDistanceFade);
        }
        for (double b = startB; b <= centerB + GRID_RENDER_RADIUS; b += BOX_SIZE) {
            renderGridLine(poseStack, builder, normal, anchor, b, centerA - GRID_RENDER_RADIUS, centerA + GRID_RENDER_RADIUS, centerB, centerA, camPos, false, playerDistanceFade);
        }
    }

    private static void renderGridLine(PoseStack ps, VertexConsumer b, Direction n, Vec3 anc, double fix, double minV, double maxV, double fixC, double varC, @Nonnull Vec3 cam, boolean isFixA, float playerFade) {
        double gridCoord = Math.abs(fix);
        boolean isBlockBoundary = Math.abs(gridCoord - Math.round(gridCoord)) < 0.01;
        float baseAlpha = isBlockBoundary ? 0.95f : 0.62f;

        for (double v = minV; v < maxV; v += BOX_SIZE) {
            double nextV = v + BOX_SIZE;
            double dist = Math.sqrt(Math.pow(fix - fixC, 2) + Math.pow(((v + nextV) * 0.5) - varC, 2));
            
            float alpha = baseAlpha * getFade(dist, GRID_RENDER_RADIUS, GRID_FADE_DISTANCE) * playerFade;
            if (alpha <= 0) continue;

            Vec3 start = createPoint(n, anc, isFixA ? fix : v, isFixA ? v : fix);
            Vec3 end = createPoint(n, anc, isFixA ? fix : nextV, isFixA ? nextV : fix);
            Vec3i normalVecInt = n.getNormal();
            if (normalVecInt == null) continue;
            renderLine(ps, b, start, end, alpha, Vec3.atLowerCornerOf(normalVecInt), cam);
        }
    }

    // --- Box Rendering ---

    private static void renderBox(PoseStack ps, @Nonnull Vec3 camPos, Vec3 pos, VertexConsumer builder) {
        double h = BOX_SIZE / 2.0;
        Vec3[] pts = {
            new Vec3(pos.x - h, pos.y - h, pos.z - h), new Vec3(pos.x + h, pos.y - h, pos.z - h),
            new Vec3(pos.x - h, pos.y + h, pos.z - h), new Vec3(pos.x + h, pos.y + h, pos.z - h),
            new Vec3(pos.x - h, pos.y - h, pos.z + h), new Vec3(pos.x + h, pos.y - h, pos.z + h),
            new Vec3(pos.x - h, pos.y + h, pos.z + h), new Vec3(pos.x + h, pos.y + h, pos.z + h)
        };

        int[][] edges = {{0,1}, {2,3}, {4,5}, {6,7}, {0,2}, {1,3}, {4,6}, {5,7}, {0,4}, {1,5}, {2,6}, {3,7}};
        for (int[] e : edges) {
            renderLine(ps, builder, pts[e[0]], pts[e[1]], 1.0f, pts[e[0]].subtract(pos).normalize(), camPos);
        }
    }

    // --- Utils ---

    private static void renderLine(PoseStack ps, VertexConsumer b, Vec3 s, Vec3 e, float alpha, Vec3 normal, @Nonnull Vec3 cam) {
        if (s == null) return;
        Vec3 dir = e.subtract(s).normalize();
        Vec3 view = s.add(e).scale(0.5).subtract(cam).normalize();
        if (view == null) return;
        Vec3 offset = dir.cross(view).normalize().scale(GRID_LINE_THICKNESS * (s.distanceTo(cam) * 0.25 + 1.0));

        PoseStack.Pose last = ps.last();
        if (last == null) return;

        b.addVertex(last, (float)(s.x - offset.x), (float)(s.y - offset.y), (float)(s.z - offset.z)).setColor(0.94f, 0.98f, 1.0f, alpha);
        b.addVertex(last, (float)(e.x - offset.x), (float)(e.y - offset.y), (float)(e.z - offset.z)).setColor(0.94f, 0.98f, 1.0f, alpha);
        b.addVertex(last, (float)(e.x + offset.x), (float)(e.y + offset.y), (float)(e.z + offset.z)).setColor(0.94f, 0.98f, 1.0f, alpha);
        b.addVertex(last, (float)(s.x + offset.x), (float)(s.y + offset.y), (float)(s.z + offset.z)).setColor(0.94f, 0.98f, 1.0f, alpha);
    }

    private static float getFade(double dist, double rad, double fade) {
        double start = rad - fade;
        if (dist <= start) return 1.0f;
        if (dist >= rad) return 0.0f;
        double p = (dist - start) / fade;
        return (float) (1.0 - (p * p * (3.0 - 2.0 * p)));
    }

    private static double getPlaneAxis(Vec3 p, Direction n, boolean isA) {
        return switch (n.getAxis()) {
            case X -> isA ? p.y : p.z;
            case Y -> isA ? p.x : p.z;
            case Z -> isA ? p.x : p.y;
        };
    }

    private static Vec3 createPoint(Direction n, Vec3 anc, double a, double b) {
        return switch (n.getAxis()) {
            case X -> new Vec3(anc.x, a, b);
            case Y -> new Vec3(a, anc.y, b);
            case Z -> new Vec3(a, b, anc.z);
        };
    }

    private static double snapToGrid(double val) {
        return Mth.floor(val / BOX_SIZE) * BOX_SIZE;
    }
}