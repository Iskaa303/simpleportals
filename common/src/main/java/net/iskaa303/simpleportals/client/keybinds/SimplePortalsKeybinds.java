package net.iskaa303.simpleportals.client.keybinds;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Portal Stick keybindings.
 */
public final class SimplePortalsKeybinds {

    public static final String KEY_CATEGORY = "key.category.simpleportals";

    // Default key codes
    public static final int DEFAULT_MODE_WHEEL = GLFW.GLFW_KEY_TAB;
    public static final int DEFAULT_SNAP_GRID = GLFW.GLFW_KEY_LEFT_SHIFT;
    public static final int DEFAULT_SNAP_POINT = GLFW.GLFW_KEY_LEFT_CONTROL;

    // Current (possibly user-modified) key codes — loaded from config
    private static int modeWheel = DEFAULT_MODE_WHEEL;
    private static int snapGrid = DEFAULT_SNAP_GRID;
    private static int snapPoint = DEFAULT_SNAP_POINT;

    public static int getModeWheel() { return modeWheel; }
    public static int getSnapGrid() { return snapGrid; }
    public static int getSnapPoint() { return snapPoint; }

    public static void setModeWheel(int key) { modeWheel = key; }
    public static void setSnapGrid(int key) { snapGrid = key; }
    public static void setSnapPoint(int key) { snapPoint = key; }

    /** Check if the physical key is pressed right now. */
    public static boolean isDown(int keyCode) {
        if (keyCode < 0) return false;
        long window = Minecraft.getInstance().getWindow().getWindow();
        if (window == 0) return false;
        return InputConstants.isKeyDown(window, keyCode);
    }

    /** Get a human-readable component for a key code. */
    public static Component getKeyName(int keyCode) {
        if (keyCode < 0) return Component.translatable("key.simpleportals.unbound");
        return InputConstants.getKey(keyCode, -1).getDisplayName();
    }

    /** All key codes (for iteration). */
    public static List<Integer> allKeys() {
        return List.of(modeWheel, snapGrid, snapPoint);
    }

    /** All default key codes. */
    public static List<Integer> defaultKeys() {
        return List.of(DEFAULT_MODE_WHEEL, DEFAULT_SNAP_GRID, DEFAULT_SNAP_POINT);
    }

    public static void resetToDefaults() {
        modeWheel = DEFAULT_MODE_WHEEL;
        snapGrid = DEFAULT_SNAP_GRID;
        snapPoint = DEFAULT_SNAP_POINT;
    }

    private SimplePortalsKeybinds() {}
}
