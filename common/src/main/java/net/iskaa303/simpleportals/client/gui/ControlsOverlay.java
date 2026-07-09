package net.iskaa303.simpleportals.client.gui;

import net.iskaa303.simpleportals.client.targeting.TargetSelector;
import net.iskaa303.simpleportals.config.OverlayPosition;
import net.iskaa303.simpleportals.config.SimplePortalsConfig;
import net.iskaa303.simpleportals.registry.SimplePortalsItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** HUD controls overlay for Point Stick and Connection Stick. */
public final class ControlsOverlay {

    private ControlsOverlay() {}

    public static void render(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        var pointStick = SimplePortalsItems.POINT_STICK.get();
        var connStick = SimplePortalsItems.CONNECTION_STICK.get();
        if (pointStick == null && connStick == null) return;

        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        boolean hasPointStick = pointStick != null && (main.is(pointStick) || off.is(pointStick));
        boolean hasConnStick = connStick != null && (main.is(connStick) || off.is(connStick));
        if (!hasPointStick && !hasConnStick) return;

        Font font = mc.font;
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        String[] lines;
        if (hasConnStick) {
            lines = new String[]{
                    Component.translatable("controls.simpleportals.connection_stick.line1").getString(),
                    Component.translatable("controls.simpleportals.connection_stick.line2").getString()
            };
        } else {
            lines = new String[]{
                    Component.translatable("controls.simpleportals.point_stick.line1").getString(),
                    Component.translatable("controls.simpleportals.point_stick.line2").getString()
            };
        }

        Vec3 target = TargetSelector.getCurrentTarget();
        if (target != null) {
            String fmt = "§fX: %." + SimplePortalsConfig.dotPrecision + "f"
                    + "  Y: %." + SimplePortalsConfig.dotPrecision + "f"
                    + "  Z: %." + SimplePortalsConfig.dotPrecision + "f";
            String coordLine = String.format(fmt, target.x, target.y, target.z);
            String[] tmp = new String[lines.length + 1];
            System.arraycopy(lines, 0, tmp, 0, lines.length);
            tmp[lines.length] = coordLine;
            lines = tmp;
        }

        int textW = 0;
        for (String line : lines) {
            int w = font.width(line);
            if (w > textW) textW = w;
        }
        int lineH = font.lineHeight;
        int totalH = lines.length * lineH;

        OverlayPosition pos = SimplePortalsConfig.overlayPosition;
        int x = pos.getX(screenW, textW);
        int y = pos.getY(screenH, totalH);

        for (int i = 0; i < lines.length; i++) {
            guiGraphics.drawString(font, lines[i], x, y + i * lineH, 0xFFFFFF, true);
        }
    }
}
