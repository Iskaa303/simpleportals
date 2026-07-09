package net.iskaa303.simpleportals.client.targeting;

import net.iskaa303.simpleportals.client.render.RenderConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Handles target point selection: raycasting from the player's eye,
 * grid snapping, surface snapping, saved-point snapping (Ctrl), and
 * smooth interpolation of the render position.
 */
public final class TargetSelector {

    private static Vec3 smoothedRenderPos;
    private static BlockHitResult latestHitResult;

    private TargetSelector() {}

    /**
     * Compute the current target position based on where the player is looking.
     * Applies modifiers:
     * - Ctrl held + saved points exist → snap to nearest saved point
     * - Shift held → snap to block surface or grid
     * - Otherwise → free aim
     */
    public static Vec3 getTarget(@Nonnull LocalPlayer player, @Nonnull Camera camera, float partialTicks) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return null;

        Vec3 eyePos = camera.getPosition();
        Vec3 lookVec = player.getViewVector(partialTicks);
        Vec3 reachVec = eyePos.add(lookVec.scale(RenderConstants.REACH));

        latestHitResult = level.clip(
                new ClipContext(eyePos, reachVec, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player)
        );
        BlockHitResult hitResult = latestHitResult;

        Vec3 targetPos = hitResult.getType() != HitResult.Type.MISS
                ? hitResult.getLocation()
                : reachVec;

        List<Vec3> savedPoints = getSavedPoints(player);
        if (Screen.hasControlDown() && savedPoints != null && !savedPoints.isEmpty()) {
            Vec3 nearest = getNearestPoint(targetPos, savedPoints);
            if (nearest != null) targetPos = nearest;
        } else if (player.isShiftKeyDown()) {
            targetPos = hitResult.getType() != HitResult.Type.MISS
                    ? snapToSurface(hitResult)
                    : snapToGrid(targetPos);
        }

        boolean shouldSmooth = player.isShiftKeyDown() || Screen.hasControlDown();
        return smoothRenderPos(targetPos, shouldSmooth);
    }

    /** Returns the last smoothed render position. Called from server-side item logic. */
    public static Vec3 getCurrentTarget() {
        return smoothedRenderPos;
    }

    /** Returns the latest block hit result from the last {@link #getTarget} call. */
    public static BlockHitResult getLastHitResult() {
        return latestHitResult;
    }

    // --- Internal helpers ---

    @SuppressWarnings("DataFlowIssue")
    private static List<Vec3> getSavedPoints(LocalPlayer player) {
        var stick = net.iskaa303.simpleportals.registry.SimplePortalsItems.DEBUG_STICK.get();
        if (stick == null) return null;
        var mainStack = player.getMainHandItem();
        var offStack = player.getOffhandItem();
        var stickStack = mainStack.is(stick) ? mainStack : offStack;
        if (!stickStack.is(stick)) return null;
        return net.iskaa303.simpleportals.item.DebugStick.getPoints(stickStack);
    }

    private static Vec3 snapToGrid(@Nonnull Vec3 pos) {
        return new Vec3(
                Mth.floor(pos.x / RenderConstants.BOX_SIZE) * RenderConstants.BOX_SIZE,
                Mth.floor(pos.y / RenderConstants.BOX_SIZE) * RenderConstants.BOX_SIZE,
                Mth.floor(pos.z / RenderConstants.BOX_SIZE) * RenderConstants.BOX_SIZE
        );
    }

    private static Vec3 snapToSurface(@Nonnull BlockHitResult hitResult) {
        Vec3 pos = hitResult.getLocation();
        Direction face = hitResult.getDirection();
        double half = RenderConstants.BOX_SIZE / 2.0;

        double x = face.getAxis() == Direction.Axis.X
                ? pos.x + (face.getStepX() * half)
                : Math.round(pos.x / RenderConstants.BOX_SIZE) * RenderConstants.BOX_SIZE;
        double y = face.getAxis() == Direction.Axis.Y
                ? pos.y + (face.getStepY() * half)
                : Math.round(pos.y / RenderConstants.BOX_SIZE) * RenderConstants.BOX_SIZE;
        double z = face.getAxis() == Direction.Axis.Z
                ? pos.z + (face.getStepZ() * half)
                : Math.round(pos.z / RenderConstants.BOX_SIZE) * RenderConstants.BOX_SIZE;

        return new Vec3(x, y, z);
    }

    private static Vec3 smoothRenderPos(@Nonnull Vec3 targetPos, boolean shouldSmooth) {
        if (!shouldSmooth || smoothedRenderPos == null || smoothedRenderPos.distanceToSqr(targetPos) > 4.0) {
            smoothedRenderPos = targetPos;
            return targetPos;
        }
        smoothedRenderPos = smoothedRenderPos.lerp(targetPos, RenderConstants.SNAP_SMOOTHING);
        return smoothedRenderPos;
    }

    private static Vec3 getNearestPoint(@Nonnull Vec3 target, @Nonnull List<Vec3> points) {
        Vec3 nearest = null;
        double minSqrDist = Double.MAX_VALUE;
        for (Vec3 point : points) {
            double distSqr = point.distanceToSqr(target);
            if (distSqr < minSqrDist) {
                minSqrDist = distSqr;
                nearest = point;
            }
        }
        return nearest;
    }
}
