package net.iskaa303.simpleportals.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.iskaa303.simpleportals.Constants;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Mod configuration loaded from {@code config/simpleportals.json}.
 * Uses Gson (bundled with Minecraft) — no extra dependencies.
 */
public class ConfigData {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ConfigData INSTANCE = new ConfigData();
    private static Path configDir;

    public OverlayPosition overlayPosition = OverlayPosition.BOTTOM_RIGHT;

    private ConfigData() {}

    public static void load(Path configDir) {
        ConfigData.configDir = configDir;
        Path file = configDir.resolve(Constants.MOD_ID + ".json");
        if (Files.isRegularFile(file)) {
            try (Reader r = Files.newBufferedReader(file)) {
                INSTANCE = GSON.fromJson(r, ConfigData.class);
            } catch (Exception e) {
                Constants.LOG.warn("Failed to load config, using defaults", e);
            }
        } else {
            save();
        }
    }

    public static void save() {
        if (configDir == null) return;
        Path file = configDir.resolve(Constants.MOD_ID + ".json");
        try {
            Files.createDirectories(configDir);
            GSON.toJson(INSTANCE, Files.newBufferedWriter(file));
        } catch (Exception e) {
            Constants.LOG.warn("Failed to save config", e);
        }
    }

    public static void setOverlayPosition(OverlayPosition pos) {
        INSTANCE.overlayPosition = pos;
        save();
    }

    public static ConfigData get() { return INSTANCE; }
}
