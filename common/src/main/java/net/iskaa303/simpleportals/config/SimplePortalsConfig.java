package net.iskaa303.simpleportals.config;

import net.iskaa303.simpleportals.Constants;
import net.iskaa303.simpleportals.client.keybinds.SimplePortalsKeybinds;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class SimplePortalsConfig {

    private static Path configDir;

    public static OverlayPosition overlayPosition = OverlayPosition.BOTTOM_RIGHT;
    public static int dotPrecision = 2;

    public static void load(Path dir) {
        configDir = dir;
        Path file = dir.resolve(Constants.MOD_ID + ".toml");
        if (Files.isRegularFile(file)) {
            try {
                List<String> lines = Files.readAllLines(file);
                for (String line : lines) {
                    line = line.strip();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    int eq = line.indexOf('=');
                    if (eq < 0) continue;
                    String key = line.substring(0, eq).strip();
                    String val = line.substring(eq + 1).strip();
                    switch (key) {
                        case "overlayPosition" -> {
                            String name = val;
                            if (name.startsWith("\"") && name.endsWith("\""))
                                name = name.substring(1, name.length() - 1);
                            for (OverlayPosition p : OverlayPosition.values()) {
                                if (p.getName().equals(name)) {
                                    overlayPosition = p;
                                    break;
                                }
                            }
                        }
                        case "dotPrecision" -> {
                            try { dotPrecision = Integer.parseInt(val); } catch (NumberFormatException ignored) {}
                        }
                        case "keyModeWheel" -> {
                            try { SimplePortalsKeybinds.setModeWheel(Integer.parseInt(val)); } catch (NumberFormatException ignored) {}
                        }
                        case "keySnapGrid" -> {
                            try { SimplePortalsKeybinds.setSnapGrid(Integer.parseInt(val)); } catch (NumberFormatException ignored) {}
                        }
                        case "keySnapPoint" -> {
                            try { SimplePortalsKeybinds.setSnapPoint(Integer.parseInt(val)); } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            } catch (Exception e) {
                Constants.LOG.warn("Failed to parse config, re-saving defaults", e);
                save();
            }
        } else {
            save();
        }
    }

    public static void save() {
        if (configDir == null) {
            Constants.LOG.warn("Cannot save config: configDir is null");
            return;
        }
        Path file = configDir.resolve(Constants.MOD_ID + ".toml");
        try {
            Files.createDirectories(configDir);
            StringBuilder sb = new StringBuilder();
            sb.append("# Simple Portals Configuration\n");
            sb.append("\n");
            sb.append("# Options: top_left, top_center, top_right, mid_left, mid_center, mid_right, bottom_left, bottom_center, bottom_right\n");
            sb.append("overlayPosition = \"").append(overlayPosition.getName()).append("\"\n");
            sb.append("\n");
            sb.append("# Integer value\n");
            sb.append("dotPrecision = ").append(dotPrecision).append("\n");
            sb.append("\n");
            sb.append("# Keybindings (GLFW key codes)\n");
            sb.append("keyModeWheel = ").append(SimplePortalsKeybinds.getModeWheel()).append("\n");
            sb.append("keySnapGrid = ").append(SimplePortalsKeybinds.getSnapGrid()).append("\n");
            sb.append("keySnapPoint = ").append(SimplePortalsKeybinds.getSnapPoint()).append("\n");
            try (BufferedWriter w = Files.newBufferedWriter(file)) {
                w.write(sb.toString());
            }
        } catch (Exception e) {
            Constants.LOG.warn("Failed to save config", e);
        }
    }

    public static void reset() {
        overlayPosition = OverlayPosition.BOTTOM_RIGHT;
        dotPrecision = 2;
        SimplePortalsKeybinds.resetToDefaults();
        save();
    }
}
