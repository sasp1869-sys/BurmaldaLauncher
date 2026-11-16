package ru.burmalda.client;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

final class BurmaldaSettings {
    static boolean hud = true;
    static boolean fps = true;
    static boolean coordinates = true;
    static boolean watermark = true;
    static boolean panel = true;
    static boolean rgb = true;
    static boolean compact = false;
    static boolean performance = false;
    static boolean fullbright = false;
    static boolean sprint = false;
    static int hudX = 8;
    static int hudY = 8;
    static int hueSpeed = 2;

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("burmalda.properties");
    }

    static void load() {
        Properties p = new Properties();
        Path path = file();
        if (!Files.exists(path)) return;
        try (Reader reader = Files.newBufferedReader(path)) {
            p.load(reader);
            hud = Boolean.parseBoolean(p.getProperty("hud", "true"));
            fps = Boolean.parseBoolean(p.getProperty("fps", "true"));
            coordinates = Boolean.parseBoolean(p.getProperty("coordinates", "true"));
            watermark = Boolean.parseBoolean(p.getProperty("watermark", "true"));
            panel = Boolean.parseBoolean(p.getProperty("panel", "true"));
            rgb = Boolean.parseBoolean(p.getProperty("rgb", "true"));
            compact = Boolean.parseBoolean(p.getProperty("compact", "false"));
            performance = Boolean.parseBoolean(p.getProperty("performance", "false"));
            fullbright = Boolean.parseBoolean(p.getProperty("fullbright", "false"));
            sprint = Boolean.parseBoolean(p.getProperty("sprint", "false"));
            hudX = Integer.parseInt(p.getProperty("hudX", "8"));
            hudY = Integer.parseInt(p.getProperty("hudY", "8"));
            hueSpeed = Integer.parseInt(p.getProperty("hueSpeed", "2"));
        } catch (IOException | NumberFormatException ignored) {
            // Defaults are kept if the settings file is damaged.
        }
    }

    static void save() {
        Properties p = new Properties();
        p.setProperty("hud", Boolean.toString(hud));
        p.setProperty("fps", Boolean.toString(fps));
        p.setProperty("coordinates", Boolean.toString(coordinates));
        p.setProperty("watermark", Boolean.toString(watermark));
        p.setProperty("panel", Boolean.toString(panel));
        p.setProperty("rgb", Boolean.toString(rgb));
        p.setProperty("compact", Boolean.toString(compact));
        p.setProperty("performance", Boolean.toString(performance));
        p.setProperty("fullbright", Boolean.toString(fullbright));
        p.setProperty("sprint", Boolean.toString(sprint));
        p.setProperty("hudX", Integer.toString(hudX));
        p.setProperty("hudY", Integer.toString(hudY));
        p.setProperty("hueSpeed", Integer.toString(hueSpeed));
        try {
            Files.createDirectories(file().getParent());
            try (Writer writer = Files.newBufferedWriter(file())) {
                p.store(writer, "Burmalda Client settings");
            }
        } catch (IOException ignored) {
            // The client remains usable if settings cannot be saved.
        }
    }

    private BurmaldaSettings() {}
}
