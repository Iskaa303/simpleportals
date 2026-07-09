package net.iskaa303.simpleportals.platform;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class SimplePortalsAgnosFabric extends SimplePortalsAgnos {

    static {
        SimplePortalsAgnos.delegate = new SimplePortalsAgnosFabric();
    }

    @Override
    protected Path getConfigDirectoryAgnos() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
