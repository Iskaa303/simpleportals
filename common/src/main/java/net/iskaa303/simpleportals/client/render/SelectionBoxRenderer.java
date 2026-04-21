package net.iskaa303.simpleportals.client.render;

import javax.annotation.Nonnull;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.iskaa303.simpleportals.item.DebugStick;
import net.iskaa303.simpleportals.registry.SimplePortalsItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class SelectionBoxRenderer {
    private static final double REACH = 10.0;
    private static final float SIZE = 0.25f;

    private SelectionBoxRenderer() {}

    public static void render(@Nonnull PoseStack poseStack, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        if (isHoldingDebugStick(player)) {
            Camera camera = mc.gameRenderer.getMainCamera();
            Vec3 eyePos = camera.getPosition();
            Vec3 lookVec = player.getViewVector(partialTicks);
            Vec3 reachVec = eyePos.add(lookVec.x * REACH, lookVec.y * REACH, lookVec.z * REACH);
            if (reachVec == null) return;

            ClientLevel level = mc.level;
            if (level == null) return;
            BlockHitResult hitResult = level.clip(new ClipContext(
                eyePos,
                reachVec,
                Block.OUTLINE,
                Fluid.NONE,
                player
            ));

            Vec3 renderPos;
            if (hitResult.getType() != HitResult.Type.MISS) {
                renderPos = hitResult.getLocation();
            } else {
                renderPos = reachVec;
            }

            renderBox(poseStack, mc, renderPos);
        }
    }

    private static boolean isHoldingDebugStick(LocalPlayer player) {
        DebugStick debugStick = SimplePortalsItems.DEBUG_STICK.get();
        if (debugStick == null) return false;

        return player.getMainHandItem().is(debugStick)
            || player.getOffhandItem().is(debugStick);
    }

    private static void renderBox(@Nonnull PoseStack poseStack, Minecraft mc, Vec3 pos) {
        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();

        poseStack.pushPose();
        try {
            poseStack.translate(
                pos.x - camPos.x - (SIZE / 2),
                pos.y - camPos.y - (SIZE / 2),
                pos.z - camPos.z - (SIZE / 2)
            );

            RenderType renderType = RenderType.lines();
            if (renderType == null) return;
            VertexConsumer builder = mc.renderBuffers().bufferSource().getBuffer(renderType);
            if (builder == null) return;

            LevelRenderer.renderLineBox(
                poseStack,
                builder,
                0, 0, 0,
                SIZE, SIZE, SIZE,
                1.0f, 1.0f, 1.0f, 1.0f
            );

            mc.renderBuffers().bufferSource().endBatch(renderType);
        } finally {
            poseStack.popPose();
        }
    }
}
