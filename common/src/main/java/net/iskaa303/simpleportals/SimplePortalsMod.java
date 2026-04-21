package net.iskaa303.simpleportals;

import javax.annotation.Nonnull;

import net.iskaa303.simpleportals.registry.SimplePortalsItems;
import net.minecraft.resources.ResourceLocation;

public class SimplePortalsMod {
    private SimplePortalsMod() {}

    public static void init() {
        SimplePortalsItems.bootstrap();
    }

    public static ResourceLocation path(final @Nonnull String path) {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path);
    }
}