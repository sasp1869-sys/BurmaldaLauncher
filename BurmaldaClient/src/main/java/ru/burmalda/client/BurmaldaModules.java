package ru.burmalda.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

final class BurmaldaModules {
    enum Category {
        MODULES("Modules"), CONFIG("Config"), GUI("GUI"), HUD("HUD"),
        FRIENDS("Friends"), MACROS("Macros"), PROFILES("Profiles"),
        BARITONE("Baritone"), COMBAT("Combat"), PLAYER("Player"),
        MOVEMENT("Movement"), RENDER("Render"), WORLD("World"),
        MISC("Misc"), SEARCH("Search");

        final String title;
        Category(String title) { this.title = title; }
    }

    static final class Module {
        final String name;
        final Category category;
        final String description;
        private final BooleanSupplier getter;
        private final Consumer<Boolean> setter;

        Module(String name, Category category, String description,
               BooleanSupplier getter, Consumer<Boolean> setter) {
            this.name = name;
            this.category = category;
            this.description = description;
            this.getter = getter;
            this.setter = setter;
        }

        boolean enabled() { return getter.getAsBoolean(); }
        void toggle() { setter.accept(!enabled()); BurmaldaSettings.save(); }
    }

    private static final List<Module> MODULES = new ArrayList<>();

    static {
        add("HUD", Category.HUD, "Показывать или скрывать интерфейс",
                () -> BurmaldaSettings.hud, v -> BurmaldaSettings.hud = v);
        add("FPS", Category.HUD, "Счётчик кадров в секунду",
                () -> BurmaldaSettings.fps, v -> BurmaldaSettings.fps = v);
        add("Coordinates", Category.HUD, "Координаты игрока XYZ",
                () -> BurmaldaSettings.coordinates, v -> BurmaldaSettings.coordinates = v);
        add("Watermark", Category.HUD, "Название Burmalda Client",
                () -> BurmaldaSettings.watermark, v -> BurmaldaSettings.watermark = v);
        add("HUD Panel", Category.HUD, "Полупрозрачная подложка HUD",
                () -> BurmaldaSettings.panel, v -> BurmaldaSettings.panel = v);
        add("RGB", Category.GUI, "Цветовая анимация интерфейса",
                () -> BurmaldaSettings.rgb, v -> BurmaldaSettings.rgb = v);
        add("Compact HUD", Category.HUD, "Уменьшенный размер HUD",
                () -> BurmaldaSettings.compact, v -> BurmaldaSettings.compact = v);
        add("Performance Display", Category.HUD, "Компактное отображение элементов",
                () -> BurmaldaSettings.performance, v -> BurmaldaSettings.performance = v);
        add("Fullbright", Category.RENDER, "Повышенная яркость только на клиенте",
                () -> BurmaldaSettings.fullbright, v -> BurmaldaSettings.fullbright = v);
        add("Sprint", Category.MOVEMENT, "Автоматически включает обычный спринт при движении вперёд",
                () -> BurmaldaSettings.sprint, v -> BurmaldaSettings.sprint = v);
        add("Auto Walk", Category.MOVEMENT, "Удерживает движение вперёд, пока модуль включён",
                () -> BurmaldaSettings.autoWalk, v -> BurmaldaSettings.autoWalk = v);
        add("Auto Jump", Category.MOVEMENT, "Включает стандартную настройку автоматического прыжка Minecraft",
                () -> BurmaldaSettings.autoJump, v -> BurmaldaSettings.autoJump = v);
        add("Ping", Category.HUD, "Показывает задержку до сервера",
                () -> BurmaldaSettings.pingHud, v -> BurmaldaSettings.pingHud = v);
        add("Direction", Category.HUD, "Показывает направление взгляда",
                () -> BurmaldaSettings.directionHud, v -> BurmaldaSettings.directionHud = v);
        add("Speed", Category.HUD, "Показывает горизонтальную скорость",
                () -> BurmaldaSettings.speedHud, v -> BurmaldaSettings.speedHud = v);
        add("Biome", Category.HUD, "Показывает текущий биом",
                () -> BurmaldaSettings.biomeHud, v -> BurmaldaSettings.biomeHud = v);
        add("Armor HUD", Category.HUD, "Показывает прочность надетой брони",
                () -> BurmaldaSettings.armorHud, v -> BurmaldaSettings.armorHud = v);
        add("Keystrokes", Category.HUD, "Показывает нажатые WASD, прыжок и спринт",
                () -> BurmaldaSettings.keystrokesHud, v -> BurmaldaSettings.keystrokesHud = v);
        add("Dimension", Category.HUD, "Показывает текущее измерение",
                () -> BurmaldaSettings.dimensionHud, v -> BurmaldaSettings.dimensionHud = v);
        add("Light Level", Category.HUD, "Показывает освещение под игроком",
                () -> BurmaldaSettings.lightHud, v -> BurmaldaSettings.lightHud = v);
        add("World Clock", Category.HUD, "Показывает игровое время мира",
                () -> BurmaldaSettings.clockHud, v -> BurmaldaSettings.clockHud = v);
        add("Better Tooltips", Category.RENDER, "Добавляет ID предмета и прочность в подсказку",
                () -> BurmaldaSettings.betterTooltips, v -> BurmaldaSettings.betterTooltips = v);
        add("Item Highlight", Category.HUD, "Показывает выбранный предмет над панелью быстрого доступа",
                () -> BurmaldaSettings.itemHighlight, v -> BurmaldaSettings.itemHighlight = v);
    }

    private static void add(String name, Category category, String description,
                            BooleanSupplier getter, Consumer<Boolean> setter) {
        MODULES.add(new Module(name, category, description, getter, setter));
    }

    static List<Module> all() { return List.copyOf(MODULES); }

    private BurmaldaModules() {}
}
