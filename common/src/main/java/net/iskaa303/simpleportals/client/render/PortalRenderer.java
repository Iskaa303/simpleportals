package net.iskaa303.simpleportals.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import foundry.veil.api.client.render.rendertype.VeilRenderType;
import net.iskaa303.simpleportals.Constants;
import net.iskaa303.simpleportals.SimplePortalsMod;
import net.iskaa303.simpleportals.entity.PortalEntity;
import net.iskaa303.simpleportals.entity.PortalWorldData;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import java.util.Collection;

/**
 * Renders all PortalEntity instances using the same Veil render type as the selection interface.
 */
public final class PortalRenderer {

    private static final double RENDER_DISTANCE = 64.0;
    private static final double RENDER_DISTANCE_SQR = RENDER_DISTANCE * RENDER_DISTANCE;

    private static final ResourceLocation RENDER_TYPE_LOC = SimplePortalsMod.path("portal_render");

    private PortalRenderer() {}

    public static void renderAll(@Nonnull PoseStack poseStack, @Nonnull Camera camera) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Vec3 camPos = camera.getPosition();
        Collection<PortalEntity> portals = PortalWorldData.CLIENT_PORTALS.values();
        if (portals.isEmpty()) return;

        RenderType renderType = VeilRenderType.get(RENDER_TYPE_LOC, "simpleportals:item/portal_stick");
        if (renderType == null) return;

        VertexConsumer builder = mc.renderBuffers().bufferSource().getBuffer(renderType);

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        for (PortalEntity portal : portals) {
            Vec3 centroid = portal.getCentroid();
            if (centroid.distanceToSqr(camPos) > RENDER_DISTANCE_SQR) continue;
            render(poseStack, builder, portal, camPos);
        }

        mc.renderBuffers().bufferSource().endBatch(renderType);
        poseStack.popPose();
    }

    private static void render(@Nonnull PoseStack ps, @Nonnull VertexConsumer builder,
                               @Nonnull PortalEntity portal, @Nonnull Vec3 camPos) {
        java.util.List<Vec3> verts = portal.getVertices();
        int n = verts.size();
        if (n < 3) return;

        PoseStack.Pose last = ps.last();
        if (last == null) return;

        float r = portal.getR();
        float g = portal.getG();
        float b = portal.getB();
        float a = 1.0f;

        Vec3 v0 = verts.get(0);
        // Single-sided triangle fan (3 verts per tri, front face only)
        for (int i = 1; i < n - 1; i++) {
            Vec3 v1 = verts.get(i);
            Vec3 v2 = verts.get(i + 1);
            builder.addVertex(last, (float) v0.x, (float) v0.y, (float) v0.z).setColor(r, g, b, a);
            builder.addVertex(last, (float) v1.x, (float) v1.y, (float) v1.z).setColor(r, g, b, a);
            builder.addVertex(last, (float) v2.x, (float) v2.y, (float) v2.z).setColor(r, g, b, a);
        }
    }
}
