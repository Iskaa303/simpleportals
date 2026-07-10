package net.iskaa303.simpleportals.platform;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.nio.file.Path;

/**
 * Platform abstraction.
 * Provides loader-specific services without common code knowing the loader.
 */
public abstract class SimplePortalsAgnos {

    public static SimplePortalsAgnos delegate;

    static {
        try {
            Class.forName("net.iskaa303.simpleportals.platform.SimplePortalsAgnosFabric");
        } catch (Throwable ignored) {
        }
        try {
            Class.forName("net.iskaa303.simpleportals.platform.SimplePortalsAgnosNeoForge");
        } catch (Throwable ignored) {
        }
    }

    public static Path getConfigDirectory() {
        return delegate.getConfigDirectoryAgnos();
    }

    protected abstract Path getConfigDirectoryAgnos();
}
