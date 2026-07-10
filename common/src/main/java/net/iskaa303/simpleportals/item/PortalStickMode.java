package net.iskaa303.simpleportals.item;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Modes for the Portal Stick.
 */
public enum PortalStickMode {
    POINT("point", "P"),
    CONNECTION("connection", "C"),
    SURFACE("surface", "S");

    private final String name;
    private final String iconLetter;

    PortalStickMode(String name, String iconLetter) {
        this.name = name;
        this.iconLetter = iconLetter;
    }

    public String translationKey() {
        return "item.simpleportals.portal_stick.mode." + name;
    }

    public MutableComponent displayName() {
        return Component.translatable(translationKey());
    }

    public String iconLetter() {
        return iconLetter;
    }

    public String getSerializedName() {
        return name;
    }

    public static PortalStickMode byName(String name) {
        for (var mode : values()) {
            if (mode.name.equals(name)) return mode;
        }
        return POINT; // safe default
    }
}
