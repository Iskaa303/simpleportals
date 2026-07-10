package net.iskaa303.simpleportals.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.apache.commons.lang3.ArrayUtils;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Generic radial / pie-menu renderer.
 * Adapted from KeyBindBundles' RadialMenuRenderer (MIT License).
 */
public abstract class RadialMenuRenderer<T> {

    public static final float INNER = 40;
    public static final float OUTER = 100;
    public static final float MIDDLE_DISTANCE = (INNER + OUTER) / 2F;

    private static final float DRAWS = 300;

    // ─── Abstract hooks ───

    public abstract List<T> getEntries();
    public abstract int getCurrentlySelected();
    public abstract Component getTitle(T entry);

    /** Short text/label drawn in the center of each sector. */
    public abstract String getIconText(T entry);

    // ─── Animation state ───

    private int[] hoverGrows = new int[0];
    private long lastUpdate = System.currentTimeMillis();

    // ─── Render ───

    /**
     * Main render method. Call from HUD overlay.
     *
     * @param guiGraphics the GUI graphics context
     * @param trackMouse  whether to track mouse for hover effects
     */
    public void render(GuiGraphics guiGraphics, boolean trackMouse) {
        var entries = getEntries();
        if (entries.isEmpty()) return;

        if (hoverGrows.length < entries.size()) {
            hoverGrows = ArrayUtils.addAll(hoverGrows,
                    IntStream.range(0, entries.size() - hoverGrows.length)
                            .map(i -> 0).toArray());
        }

        int count = entries.size();
        float angleSize = 360F / count;

        float centerX = guiGraphics.guiWidth() / 2f;
        float centerY = guiGraphics.guiHeight() / 2f;
        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(centerX, centerY, 0);

        int hot = getCurrentlySelected();

        // Draw each torus sector
        for (int i = 0; i < entries.size(); i++) {
            float startAngle = -90F + 360F * (-0.5F + i) / count;
            float outer = OUTER + 10f * (hoverGrows[i] / 10f);
            boolean isSelected = (hot == i);
            drawTorus(pose, startAngle, angleSize, INNER, outer,
                    isSelected ? 0.7f : 0.3f,
                    isSelected ? 0.4f : 0.3f,
                    isSelected ? 0.45f : 0.3f,
                    isSelected ? 0.7F : 0.6F);
        }

        // Hover animation update (40 FPS)
        if (trackMouse && !Minecraft.getInstance().mouseHandler.isMouseGrabbed()) {
            int underMouse = getElementUnderMouse(false);
            long current = System.currentTimeMillis();
            if (current >= (this.lastUpdate + 1000 / 40)) {
                lastUpdate = current;
                for (int i = 0; i < entries.size(); i++) {
                    if (i == underMouse) {
                        if (hoverGrows[i] < 10) hoverGrows[i]++;
                    } else if (hoverGrows[i] > 0) {
                        hoverGrows[i]--;
                    }
                }
            }
        }

        // Draw labels on each sector
        record PosText(float x, float y, Component text, String icon) {}
        List<PosText> textToDraw = new ArrayList<>(entries.size());

        float position = 0;
        for (var entry : entries) {
            float degrees = 270 + 360 * (position++ / count);
            float angle = Mth.DEG_TO_RAD * degrees;
            float x = Mth.cos(angle) * MIDDLE_DISTANCE;
            float y = Mth.sin(angle) * MIDDLE_DISTANCE;
            textToDraw.add(new PosText(x, y, getTitle(entry), getIconText(entry)));
        }

        var font = Minecraft.getInstance().font;
        for (var td : textToDraw) {
            pose.pushPose();
            pose.translate(td.x, td.y, 0);
            pose.scale(0.6F, 0.6F, 1);

            // Draw icon letter (dummy until real assets)
            String icon = td.icon;
            Component iconComp = Component.literal(icon);
            font.drawInBatch(iconComp, -font.width(iconComp) / 2f, 0,
                    0xCCFFFFFF, true, pose.last().pose(),
                    guiGraphics.bufferSource(), net.minecraft.client.gui.Font.DisplayMode.SEE_THROUGH,
                    0, 0xF000F0);

            // Draw title below icon
            Component text = td.text;
            font.drawInBatch(text, -font.width(text) / 2f, 10,
                    0xCCFFFFFF, true, pose.last().pose(),
                    guiGraphics.bufferSource(), net.minecraft.client.gui.Font.DisplayMode.SEE_THROUGH,
                    0, 0xF000F0);

            pose.popPose();
        }

        pose.popPose();
    }

    // ─── Mouse hit detection ───

    public record MousePos(double x, double y) {}

    public MousePos getMousePos() {
        var mouse = Minecraft.getInstance().mouseHandler;
        double mouseX = mouse.xpos() * (double) Minecraft.getInstance().getWindow().getGuiScaledWidth()
                / (double) Minecraft.getInstance().getWindow().getScreenWidth();
        double mouseY = mouse.ypos() * (double) Minecraft.getInstance().getWindow().getGuiScaledHeight()
                / (double) Minecraft.getInstance().getWindow().getScreenHeight();
        return new MousePos(mouseX, mouseY);
    }

    /**
     * Returns the index of the sector under the mouse, or -1 if none.
     *
     * @param upperboundRadius if true, also returns -1 when mouse is beyond the outer ring
     */
    protected int getElementUnderMouse(boolean upperboundRadius) {
        var mouse = getMousePos();
        int count = getEntries().size();
        if (count == 0) return -1;

        var window = Minecraft.getInstance().getWindow();
        float centerX = window.getGuiScaledWidth() / 2f;
        float centerY = window.getGuiScaledHeight() / 2f;

        double xDiff = mouse.x - centerX;
        double yDiff = mouse.y - centerY;
        double distanceFromCenter = Mth.length(xDiff, yDiff);
        if (distanceFromCenter < 10) return -1;
        if (upperboundRadius && distanceFromCenter > OUTER) return -1;

        // If beyond outer, still allow selection (click-to-select)
        float angle = (float) (Mth.RAD_TO_DEG * Mth.atan2(yDiff, xDiff));
        float selectionAngle = wrapDegrees(angle + 180F / count + 90F);
        return (int) (selectionAngle * (count / 360F));
    }

    // ─── Helpers ───

    protected void clearState() {
        hoverGrows = new int[0];
        lastUpdate = System.currentTimeMillis();
    }

    // ─── Torus drawing ───

    /**
     * Draw a filled torus sector using immediate-mode BufferBuilder.
     */
    private void drawTorus(PoseStack poseStack, float startAngle, float sizeAngle,
                           float inner, float outer,
                           float red, float green, float blue, float alpha) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Matrix4f matrix = poseStack.last().pose();
        float draws = DRAWS * (sizeAngle / 360F);

        BufferBuilder buffer = Tesselator.getInstance().begin(
                VertexFormat.Mode.TRIANGLE_STRIP,
                DefaultVertexFormat.POSITION_COLOR
        );

        for (int i = 0; i <= draws; i++) {
            float degrees = startAngle + (i / DRAWS) * 360;
            float angle = Mth.DEG_TO_RAD * degrees;
            float cos = Mth.cos(angle);
            float sin = Mth.sin(angle);

            buffer.addVertex(matrix, outer * cos, outer * sin, 0)
                    .setColor(red, green, blue, alpha);
            buffer.addVertex(matrix, inner * cos, inner * sin, 0)
                    .setColor(red, green, blue, alpha);
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();
    }

    public static float wrapDegrees(float angle) {
        angle = angle % 360;
        if (angle < 0) angle += 360;
        return angle;
    }
}
