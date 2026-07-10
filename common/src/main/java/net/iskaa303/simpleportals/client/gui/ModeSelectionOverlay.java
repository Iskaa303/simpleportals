package net.iskaa303.simpleportals.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.iskaa303.simpleportals.client.keybinds.SimplePortalsKeybinds;
import net.iskaa303.simpleportals.client.render.RadialMenuRenderer;
import net.iskaa303.simpleportals.item.PortalStick;
import net.iskaa303.simpleportals.item.PortalStickMode;
import net.iskaa303.simpleportals.item.PointDataStore;
import net.iskaa303.simpleportals.client.render.RadialMenuRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * Radial mode selection overlay. Shows a 3-sector wheel for choosing stick mode.
 * Adapted from KeyBindBundles by Matyrobbrt (MIT, see licenses/KeyBindBundles.license).
 */
public class ModeSelectionOverlay extends RadialMenuRenderer<PortalStickMode> {

    public static final ModeSelectionOverlay INSTANCE = new ModeSelectionOverlay();

    @Nullable
    private PortalStickMode currentSelection;
    private boolean open;
    private PortalStickMode selectedOnOpen;

    private ModeSelectionOverlay() {}

    // ─── State ───

    public boolean isOpen() {
        return open;
    }

    /** Called when the mode wheel should open. */
    public void open() {
        if (open) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        this.currentSelection = PointDataStore.getMode(player);
        this.selectedOnOpen = this.currentSelection;
        this.open = true;
        clearState();
        // Free the mouse so the player can move the cursor to select a mode
        mc.mouseHandler.releaseMouse();
    }

    /** Applies the selected mode. */
    public void close() {
        if (!open) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player != null) {
            int underMouse = getElementUnderMouse(false);
            if (underMouse >= 0 && underMouse < getEntries().size()) {
                PortalStickMode chosen = getEntries().get(underMouse);
                if (chosen != selectedOnOpen) {
                    PointDataStore.setMode(player, chosen);
                    player.displayClientMessage(
                            Component.translatable("message.simpleportals.mode_changed", chosen.displayName()),
                            true
                    );
                }
            }
            this.currentSelection = PointDataStore.getMode(player);
        }
        this.open = false;
        clearState();
        // Re-grab the mouse if no screen is open
        if (!mc.mouseHandler.isMouseGrabbed() && mc.screen == null) {
            mc.mouseHandler.grabMouse();
        }
    }

    // ─── RadialMenuRenderer impl ───

    @Override
    public List<PortalStickMode> getEntries() {
        return Arrays.asList(PortalStickMode.values());
    }

    @Override
    public int getCurrentlySelected() {
        if (currentSelection == null) return -1;
        return currentSelection.ordinal();
    }

    @Override
    public Component getTitle(PortalStickMode entry) {
        return entry.displayName();
    }

    @Override
    public String getIconText(PortalStickMode entry) {
        return entry.iconLetter();
    }

    // ─── Tick ───

    /** Called every client tick to check key states. */
    public static void tick() {
        DragController.tick();
        SurfaceTransformController.tick();
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        boolean holding = PortalStick.isHolding(player);
        boolean wheelHeld = SimplePortalsKeybinds.isDown(SimplePortalsKeybinds.getModeWheel());

        if (holding && wheelHeld) {
            INSTANCE.open();
        } else if (INSTANCE.isOpen()) {
            INSTANCE.close();
        }
    }

    // ─── Render ───

    /**
     * Main render entry point. Called from HUD layer on both loaders.
     */
    public static void renderOverlay(GuiGraphics guiGraphics, DeltaTracker delta) {
        if (!INSTANCE.isOpen()) return;
        INSTANCE.render(guiGraphics, true);
    }
}
