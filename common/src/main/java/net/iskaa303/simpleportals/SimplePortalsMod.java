package net.iskaa303.simpleportals;

import net.iskaa303.simpleportals.platform.SimplePortalsAgnos;
import net.iskaa303.simpleportals.registry.SimplePortalsItems;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

public class SimplePortalsMod {
    private SimplePortalsMod() {}

    public static void init() {
        SimplePortalsItems.bootstrap();
        SimplePortalsAgnos.registerPortalPayloads();
        SimplePortalsAgnos.registerServerPortalHandlers();
    }

    public static ResourceLocation path(final @Nonnull String path) {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path);
    }
}