package net.iskaa303.simpleportals.client.gui;

import net.iskaa303.simpleportals.client.keybinds.SimplePortalsKeybinds;
import net.iskaa303.simpleportals.client.gui.DragController;
import net.iskaa303.simpleportals.client.gui.DragController.DragMode;
import net.iskaa303.simpleportals.client.keybinds.SimplePortalsKeybinds;
import net.iskaa303.simpleportals.client.targeting.TargetSelector;
import net.iskaa303.simpleportals.config.OverlayPosition;
import net.iskaa303.simpleportals.config.SimplePortalsConfig;
import net.iskaa303.simpleportals.item.PortalStickMode;
import net.iskaa303.simpleportals.item.PointDataStore;
import net.iskaa303.simpleportals.registry.SimplePortalsItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** HUD controls overlay for the Portal Stick. */
public final class ControlsOverlay {

    private ControlsOverlay() {}

    public static void render(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        Font font = mc.font;
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        PortalStickMode mode = PointDataStore.getMode(player);

        var lines = new java.util.ArrayList<String>();

        // Mode line
        lines.add(Component.translatable("controls.simpleportals.portal_stick.mode",
                mode.displayName()).getString());

        // Controls line (keybinding names)
        String modeWheelKey = SimplePortalsKeybinds.getKeyName(SimplePortalsKeybinds.getModeWheel()).getString();
        String snapGridKey = SimplePortalsKeybinds.getKeyName(SimplePortalsKeybinds.getSnapGrid()).getString();
        String snapPointKey = SimplePortalsKeybinds.getKeyName(SimplePortalsKeybinds.getSnapPoint()).getString();
        String toggleGridKey = SimplePortalsKeybinds.getKeyName(SimplePortalsKeybinds.getToggleGrid()).getString();
        lines.add(Component.translatable("controls.simpleportals.portal_stick.controls",
                modeWheelKey, snapGridKey, snapPointKey).getString());

        // Grid toggle state
        String gridState = SimplePortalsConfig.showGrid
                ? "§aON"
                : "§7OFF";
        lines.add(Component.translatable("controls.simpleportals.portal_stick.grid",
                toggleGridKey, gridState).getString());

        // Drag or action line
        if (DragController.isDragging()) {
            String dragTarget = switch (DragController.getDragMode()) {
                case POINT -> "point";
                case CONNECTION -> "connection";
                case SURFACE -> "surface";
                default -> "";
            };
            lines.add(Component.translatable("controls.simpleportals.portal_stick.dragging",
                    dragTarget).getString());
            lines.add(Component.translatable("controls.simpleportals.portal_stick.place_hint").getString());
        } else {
            lines.add(Component.translatable("controls.simpleportals.portal_stick.action").getString());
            lines.add(Component.translatable("controls.simpleportals.portal_stick.drag_hint").getString());
        }

        // Coordinates
        Vec3 target = TargetSelector.getCurrentTarget();
        if (target != null) {
            String fmt = "§fX: %." + SimplePortalsConfig.dotPrecision + "f"
                    + "  Y: %." + SimplePortalsConfig.dotPrecision + "f"
                    + "  Z: %." + SimplePortalsConfig.dotPrecision + "f";
            String coordLine = String.format(fmt, target.x, target.y, target.z);
            lines.add(coordLine);
        }

        // Measure and position
        int textW = 0;
        for (String line : lines) {
            int w = font.width(line);
            if (w > textW) textW = w;
        }
        int lineH = font.lineHeight;
        int totalH = lines.size() * lineH;

        OverlayPosition pos = SimplePortalsConfig.overlayPosition;
        int x = pos.getX(screenW, textW);
        int y = pos.getY(screenH, totalH);

        for (int i = 0; i < lines.size(); i++) {
            guiGraphics.drawString(font, lines.get(i), x, y + i * lineH, 0xFFFFFF, true);
        }
    }
}
