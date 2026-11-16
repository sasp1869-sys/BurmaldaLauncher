package ru.burmalda.client;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

final class BurmaldaSettings {
    static boolean hud = true;
    static boolean fps = true;
    static boolean coordinates = true;
    static boolean buildHeightHud = false;
    static boolean watermark = true;
    static boolean panel = true;
    static boolean rgb = true;
    static boolean compact = false;
    static boolean performance = false;
    static boolean fullbright = false;
    static boolean cameraTweaks = false;
    static boolean sprint = false;
    static boolean autoWalk = false;
    static boolean autoJump = false;
    static boolean pingHud = true;
    static boolean directionHud = false;
    static boolean speedHud = false;
    static boolean biomeHud = false;
    static boolean armorHud = false;
    static boolean keystrokesHud = false;
    static boolean dimensionHud = false;
    static boolean lightHud = false;
    static boolean clockHud = false;
    static boolean betterTooltips = false;
    static boolean itemHighlight = false;
    static boolean copyCoordinates = true;
    static int hudX = 8;
    static int hudY = 8;
    static int hueSpeed = 2;

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("burmalda.properties");
    }

    private static Path profilesDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("burmalda").resolve("configs");
    }

    static void load() {
        readFrom(file());
    }

    static void save() {
        writeTo(file());
    }

    static boolean saveNamed(String name) {
        Path target = namedFile(name);
        if (target == null) return false;
        writeTo(target);
        return Files.exists(target);
    }

    static boolean loadNamed(String name) {
        Path target = namedFile(name);
        if (target == null || !Files.isRegularFile(target)) return false;
        readFrom(target);
        save();
        return true;
    }

    static boolean deleteNamed(String name) {
        Path target = namedFile(name);
        if (target == null) return false;
        try {
            return Files.deleteIfExists(target);
        } catch (IOException ignored) {
            return false;
        }
    }

    static List<String> listNamed() {
        try {
            Files.createDirectories(profilesDir());
            try (Stream<Path> files = Files.list(profilesDir())) {
                return files.filter(Files::isRegularFile)
                        .map(p -> p.getFileName().toString())
                        .filter(n -> n.endsWith(".properties"))
                        .map(n -> n.substring(0, n.length() - ".properties".length()))
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .toList();
            }
        } catch (IOException ignored) {
            return List.of();
        }
    }

    private static Path namedFile(String name) {
        if (name == null || !name.matches("[A-Za-z0-9_-]{1,32}")) return null;
        return profilesDir().resolve(name + ".properties");
    }

    private static void readFrom(Path path) {
        if (!Files.isRegularFile(path)) return;
        Properties p = new Properties();
        try (Reader reader = Files.newBufferedReader(path)) {
            p.load(reader);
            hud = Boolean.parseBoolean(p.getProperty("hud", Boolean.toString(hud)));
            fps = Boolean.parseBoolean(p.getProperty("fps", Boolean.toString(fps)));
            coordinates = Boolean.parseBoolean(p.getProperty("coordinates", Boolean.toString(coordinates)));
            buildHeightHud = Boolean.parseBoolean(p.getProperty("buildHeightHud", Boolean.toString(buildHeightHud)));
            watermark = Boolean.parseBoolean(p.getProperty("watermark", Boolean.toString(watermark)));
            panel = Boolean.parseBoolean(p.getProperty("panel", Boolean.toString(panel)));
            rgb = Boolean.parseBoolean(p.getProperty("rgb", Boolean.toString(rgb)));
            compact = Boolean.parseBoolean(p.getProperty("compact", Boolean.toString(compact)));
            performance = Boolean.parseBoolean(p.getProperty("performance", Boolean.toString(performance)));
            fullbright = Boolean.parseBoolean(p.getProperty("fullbright", Boolean.toString(fullbright)));
            cameraTweaks = Boolean.parseBoolean(p.getProperty("cameraTweaks", Boolean.toString(cameraTweaks)));
            sprint = Boolean.parseBoolean(p.getProperty("sprint", Boolean.toString(sprint)));
            autoWalk = Boolean.parseBoolean(p.getProperty("autoWalk", Boolean.toString(autoWalk)));
            autoJump = Boolean.parseBoolean(p.getProperty("autoJump", Boolean.toString(autoJump)));
            pingHud = Boolean.parseBoolean(p.getProperty("pingHud", Boolean.toString(pingHud)));
            directionHud = Boolean.parseBoolean(p.getProperty("directionHud", Boolean.toString(directionHud)));
            speedHud = Boolean.parseBoolean(p.getProperty("speedHud", Boolean.toString(speedHud)));
            biomeHud = Boolean.parseBoolean(p.getProperty("biomeHud", Boolean.toString(biomeHud)));
            armorHud = Boolean.parseBoolean(p.getProperty("armorHud", Boolean.toString(armorHud)));
            keystrokesHud = Boolean.parseBoolean(p.getProperty("keystrokesHud", Boolean.toString(keystrokesHud)));
            dimensionHud = Boolean.parseBoolean(p.getProperty("dimensionHud", Boolean.toString(dimensionHud)));
            lightHud = Boolean.parseBoolean(p.getProperty("lightHud", Boolean.toString(lightHud)));
            clockHud = Boolean.parseBoolean(p.getProperty("clockHud", Boolean.toString(clockHud)));
            betterTooltips = Boolean.parseBoolean(p.getProperty("betterTooltips", Boolean.toString(betterTooltips)));
            itemHighlight = Boolean.parseBoolean(p.getProperty("itemHighlight", Boolean.toString(itemHighlight)));
            copyCoordinates = Boolean.parseBoolean(p.getProperty("copyCoordinates", Boolean.toString(copyCoordinates)));
            hudX = Integer.parseInt(p.getProperty("hudX", Integer.toString(hudX)));
            hudY = Integer.parseInt(p.getProperty("hudY", Integer.toString(hudY)));
            hueSpeed = Integer.parseInt(p.getProperty("hueSpeed", Integer.toString(hueSpeed)));
        } catch (IOException | NumberFormatException ignored) {
            // Keep current values if a settings file is damaged.
        }
    }

    private static void writeTo(Path path) {
        Properties p = new Properties();
        p.setProperty("hud", Boolean.toString(hud));
        p.setProperty("fps", Boolean.toString(fps));
        p.setProperty("coordinates", Boolean.toString(coordinates));
        p.setProperty("buildHeightHud", Boolean.toString(buildHeightHud));
        p.setProperty("watermark", Boolean.toString(watermark));
        p.setProperty("panel", Boolean.toString(panel));
        p.setProperty("rgb", Boolean.toString(rgb));
        p.setProperty("compact", Boolean.toString(compact));
        p.setProperty("performance", Boolean.toString(performance));
        p.setProperty("fullbright", Boolean.toString(fullbright));
        p.setProperty("cameraTweaks", Boolean.toString(cameraTweaks));
        p.setProperty("sprint", Boolean.toString(sprint));
        p.setProperty("autoWalk", Boolean.toString(autoWalk));
        p.setProperty("autoJump", Boolean.toString(autoJump));
        p.setProperty("pingHud", Boolean.toString(pingHud));
        p.setProperty("directionHud", Boolean.toString(directionHud));
        p.setProperty("speedHud", Boolean.toString(speedHud));
        p.setProperty("biomeHud", Boolean.toString(biomeHud));
        p.setProperty("armorHud", Boolean.toString(armorHud));
        p.setProperty("keystrokesHud", Boolean.toString(keystrokesHud));
        p.setProperty("dimensionHud", Boolean.toString(dimensionHud));
        p.setProperty("lightHud", Boolean.toString(lightHud));
        p.setProperty("clockHud", Boolean.toString(clockHud));
        p.setProperty("betterTooltips", Boolean.toString(betterTooltips));
        p.setProperty("itemHighlight", Boolean.toString(itemHighlight));
        p.setProperty("copyCoordinates", Boolean.toString(copyCoordinates));
        p.setProperty("hudX", Integer.toString(hudX));
        p.setProperty("hudY", Integer.toString(hudY));
        p.setProperty("hueSpeed", Integer.toString(hueSpeed));
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                p.store(writer, "Burmalda Client settings");
            }
        } catch (IOException ignored) {
            // The client remains usable if settings cannot be saved.
        }
    }

    private BurmaldaSettings() {}
}
