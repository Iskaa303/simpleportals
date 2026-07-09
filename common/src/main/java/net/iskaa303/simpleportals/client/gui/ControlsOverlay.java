package net.iskaa303.simpleportals.client.gui;

import net.iskaa303.simpleportals.client.targeting.TargetSelector;
import net.iskaa303.simpleportals.config.OverlayPosition;
import net.iskaa303.simpleportals.config.ConfigData;
import net.iskaa303.simpleportals.registry.SimplePortalsItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.text.DecimalFormat;

/**
 * Renders the control hints and cursor coordinates on the HUD
 * when the player is holding the portal stick.
 */
public final class ControlsOverlay {

    private static final DecimalFormat FMT = new DecimalFormat("#.##");

    private ControlsOverlay() {}

    public static void render(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        var stick = SimplePortalsItems.PORTAL_STICK.get();
        if (stick == null) return;
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        if (!main.is(stick) && !off.is(stick)) return;

        Font font = mc.font;
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        // Build overlay lines
        String[] lines = {
                "§fRight Click: Create/Delete Point",
                "§fShift: Snap to Grid   §fCtrl: Snap to Point"
        };

        Vec3 target = TargetSelector.getCurrentTarget();
        if (target != null) {
            String coordLine = "§fX: " + FMT.format(target.x)
                    + "  Y: " + FMT.format(target.y)
                    + "  Z: " + FMT.format(target.z);
            String[] tmp = new String[lines.length + 1];
            System.arraycopy(lines, 0, tmp, 0, lines.length);
            tmp[lines.length] = coordLine;
            lines = tmp;
        }

        // Measure the widest line
        int textW = 0;
        for (String line : lines) {
            int w = font.width(line);
            if (w > textW) textW = w;
        }
        int lineH = font.lineHeight;
        int totalH = lines.length * lineH;

        OverlayPosition pos = ConfigData.get().overlayPosition;
        int x = pos.getX(screenW, textW);
        int y = pos.getY(screenH, totalH);

        for (int i = 0; i < lines.length; i++) {
            guiGraphics.drawString(font, lines[i], x, y + i * lineH, 0xFFFFFF, true);
        }
    }
}
