package net.iskaa303.simpleportals.platform;

import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

public class SimplePortalsAgnosNeoForge extends SimplePortalsAgnos {

    static {
        SimplePortalsAgnos.delegate = new SimplePortalsAgnosNeoForge();
    }

    @Override
    protected Path getConfigDirectoryAgnos() {
        return FMLPaths.CONFIGDIR.get();
    }
}
