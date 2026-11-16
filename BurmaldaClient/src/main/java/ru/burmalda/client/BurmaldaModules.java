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
        MOVEMENT("Movement"), RENDER("Render"), WORLD("World"), INVENTORY("Inventory"), TARGET("Target"),
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
        add("Camera Tweaks", Category.RENDER, "Отключает тряску камеры при ходьбе; исходная настройка восстанавливается при выключении",
                () -> BurmaldaSettings.cameraTweaks, v -> BurmaldaSettings.cameraTweaks = v);
        add("Sprint", Category.MOVEMENT, "Автоматически включает обычный спринт при движении вперёд",
                () -> BurmaldaSettings.sprint, v -> BurmaldaSettings.sprint = v);
        add("Auto Walk", Category.MOVEMENT, "Удерживает движение вперёд, пока модуль включён",
                () -> BurmaldaSettings.autoWalk, v -> BurmaldaSettings.autoWalk = v);
        add("Auto Jump", Category.MOVEMENT, "Включает стандартную настройку автоматического прыжка Minecraft",
                () -> BurmaldaSettings.autoJump, v -> BurmaldaSettings.autoJump = v);
        add("Build Height", Category.HUD, "Показывает высоту игрока и верхнюю границу строительства",
                () -> BurmaldaSettings.buildHeightHud, v -> BurmaldaSettings.buildHeightHud = v);
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
        add("Potion HUD", Category.HUD, "Показывает активные эффекты и оставшееся время",
                () -> BurmaldaSettings.potionHud, v -> BurmaldaSettings.potionHud = v);
        add("Item Highlight", Category.HUD, "Показывает выбранный предмет над панелью быстрого доступа",
                () -> BurmaldaSettings.itemHighlight, v -> BurmaldaSettings.itemHighlight = v);
        add("Copy Coordinates", Category.MISC, "Копирует координаты блока по F8",
                () -> BurmaldaSettings.copyCoordinates, v -> BurmaldaSettings.copyCoordinates = v);
        add("Auto Tool", Category.PLAYER, "Выбирает лучший инструмент из хотбара при начале добычи блока",
                () -> BurmaldaSettings.autoTool, v -> BurmaldaSettings.autoTool = v);
        add("Food HUD", Category.HUD, "Показывает уровень голода и насыщения",
                () -> BurmaldaSettings.foodHud, v -> BurmaldaSettings.foodHud = v);
        add("Totem Counter", Category.HUD, "Считает тотемы бессмертия в инвентаре и хотбаре",
                () -> BurmaldaSettings.totemCounter, v -> BurmaldaSettings.totemCounter = v);
        add("Target Entity HUD", Category.HUD, "Показывает имя и тип сущности под прицелом",
                () -> BurmaldaSettings.targetEntityHud, v -> BurmaldaSettings.targetEntityHud = v);
        add("Target Block HUD", Category.HUD, "Показывает ID блока под прицелом",
                () -> BurmaldaSettings.targetBlockHud, v -> BurmaldaSettings.targetBlockHud = v);
        add("Memory HUD", Category.HUD, "Показывает использование памяти Java",
                () -> BurmaldaSettings.memoryHud, v -> BurmaldaSettings.memoryHud = v);
        add("Local Time HUD", Category.HUD, "Показывает локальное время компьютера",
                () -> BurmaldaSettings.localTimeHud, v -> BurmaldaSettings.localTimeHud = v);
        add("Day Counter", Category.HUD, "Показывает номер игрового дня",
                () -> BurmaldaSettings.dayCounterHud, v -> BurmaldaSettings.dayCounterHud = v);
        add("Weather HUD", Category.HUD, "Показывает дождь и грозу в текущем мире",
                () -> BurmaldaSettings.weatherHud, v -> BurmaldaSettings.weatherHud = v);
        add("Chunk HUD", Category.HUD, "Показывает координаты чанка и локальные координаты",
                () -> BurmaldaSettings.chunkHud, v -> BurmaldaSettings.chunkHud = v);
        add("Durability HUD", Category.HUD, "Показывает остаток прочности предмета в основной руке",
                () -> BurmaldaSettings.durabilityHud, v -> BurmaldaSettings.durabilityHud = v);
        add("Held Item HUD", Category.HUD, "Показывает название и количество предмета в основной руке",
                () -> BurmaldaSettings.heldItemHud, v -> BurmaldaSettings.heldItemHud = v);
        add("Experience HUD", Category.HUD, "Показывает уровень опыта и прогресс до следующего уровня",
                () -> BurmaldaSettings.experienceHud, v -> BurmaldaSettings.experienceHud = v);
        add("Empty Slots HUD", Category.HUD, "Показывает количество пустых слотов инвентаря",
                () -> BurmaldaSettings.emptySlotsHud, v -> BurmaldaSettings.emptySlotsHud = v);
        add("Selected Slot HUD", Category.HUD, "Показывает выбранный слот хотбара",
                () -> BurmaldaSettings.selectedSlotHud, v -> BurmaldaSettings.selectedSlotHud = v);
        add("Facing Angle HUD", Category.HUD, "Показывает точный угол поворота игрока",
                () -> BurmaldaSettings.facingAngleHud, v -> BurmaldaSettings.facingAngleHud = v);
        add("Vertical Speed HUD", Category.HUD, "Показывает вертикальную скорость игрока",
                () -> BurmaldaSettings.verticalSpeedHud, v -> BurmaldaSettings.verticalSpeedHud = v);
        add("Fall Distance HUD", Category.HUD, "Показывает текущую дистанцию падения",
                () -> BurmaldaSettings.fallDistanceHud, v -> BurmaldaSettings.fallDistanceHud = v);
        add("Air HUD", Category.HUD, "Показывает запас воздуха под водой",
                () -> BurmaldaSettings.airHud, v -> BurmaldaSettings.airHud = v);
        add("Nearby Players HUD", Category.HUD, "Считает игроков в радиусе 64 блоков",
                () -> BurmaldaSettings.nearbyPlayersHud, v -> BurmaldaSettings.nearbyPlayersHud = v);
        add("Nearby Mobs HUD", Category.HUD, "Считает мобов в радиусе 16 блоков",
                () -> BurmaldaSettings.nearbyMobsHud, v -> BurmaldaSettings.nearbyMobsHud = v);
        add("Target Distance HUD", Category.HUD, "Показывает расстояние до сущности под прицелом",
                () -> BurmaldaSettings.targetDistanceHud, v -> BurmaldaSettings.targetDistanceHud = v);
        add("Spawn Distance HUD", Category.HUD, "Показывает расстояние до точки спавна мира",
                () -> BurmaldaSettings.spawnDistanceHud, v -> BurmaldaSettings.spawnDistanceHud = v);
        add("Health HUD", Category.PLAYER, "Показывает здоровье игрока в сердцах",
                () -> BurmaldaSettings.healthHud, v -> BurmaldaSettings.healthHud = v);
        add("Absorption HUD", Category.PLAYER, "Показывает дополнительные сердца от поглощения",
                () -> BurmaldaSettings.absorptionHud, v -> BurmaldaSettings.absorptionHud = v);
        add("Offhand HUD", Category.INVENTORY, "Показывает предмет во второй руке",
                () -> BurmaldaSettings.offhandHud, v -> BurmaldaSettings.offhandHud = v);
        add("Arrow Counter", Category.INVENTORY, "Считает стрелы в инвентаре",
                () -> BurmaldaSettings.arrowCounterHud, v -> BurmaldaSettings.arrowCounterHud = v);
        add("Pearl Counter", Category.INVENTORY, "Считает эндер-жемчуг в инвентаре",
                () -> BurmaldaSettings.pearlCounterHud, v -> BurmaldaSettings.pearlCounterHud = v);
        add("Golden Apple Counter", Category.INVENTORY, "Считает золотые яблоки в инвентаре",
                () -> BurmaldaSettings.appleCounterHud, v -> BurmaldaSettings.appleCounterHud = v);
        add("Shield Status", Category.INVENTORY, "Показывает прочность щита во второй или основной руке",
                () -> BurmaldaSettings.shieldStatusHud, v -> BurmaldaSettings.shieldStatusHud = v);
        add("Target Block Position", Category.TARGET, "Показывает координаты блока под прицелом",
                () -> BurmaldaSettings.targetBlockPosHud, v -> BurmaldaSettings.targetBlockPosHud = v);
        add("Target Health", Category.TARGET, "Показывает здоровье сущности под прицелом",
                () -> BurmaldaSettings.targetHealthHud, v -> BurmaldaSettings.targetHealthHud = v);
        add("Spawn Coordinates", Category.WORLD, "Показывает координаты точки спавна мира",
                () -> BurmaldaSettings.spawnCoordsHud, v -> BurmaldaSettings.spawnCoordsHud = v);
        add("Saturation HUD", Category.PLAYER, "Показывает уровень насыщения еды",
                () -> BurmaldaSettings.saturationHud, v -> BurmaldaSettings.saturationHud = v);
        add("Ground Status", Category.MOVEMENT, "Показывает, стоит ли игрок на земле",
                () -> BurmaldaSettings.groundStatusHud, v -> BurmaldaSettings.groundStatusHud = v);
        add("Sprint Status", Category.MOVEMENT, "Показывает активен ли спринт",
                () -> BurmaldaSettings.sprintStatusHud, v -> BurmaldaSettings.sprintStatusHud = v);
        add("Swimming Status", Category.MOVEMENT, "Показывает, плывёт ли игрок",
                () -> BurmaldaSettings.swimmingStatusHud, v -> BurmaldaSettings.swimmingStatusHud = v);
        add("Elytra Status", Category.MOVEMENT, "Показывает, раскрыты ли элитры в полёте",
                () -> BurmaldaSettings.elytraStatusHud, v -> BurmaldaSettings.elytraStatusHud = v);
        add("Block Light HUD", Category.WORLD, "Показывает уровень света от блоков у игрока",
                () -> BurmaldaSettings.blockLightHud, v -> BurmaldaSettings.blockLightHud = v);
        add("Sky Light HUD", Category.WORLD, "Показывает уровень небесного света у игрока",
                () -> BurmaldaSettings.skyLightHud, v -> BurmaldaSettings.skyLightHud = v);
        add("Moon Phase HUD", Category.WORLD, "Показывает текущую фазу луны",
                () -> BurmaldaSettings.moonPhaseHud, v -> BurmaldaSettings.moonPhaseHud = v);
        add("Biome Temperature", Category.WORLD, "Показывает температуру текущего биома",
                () -> BurmaldaSettings.biomeTemperatureHud, v -> BurmaldaSettings.biomeTemperatureHud = v);
        add("Inventory Usage", Category.INVENTORY, "Показывает занятые слоты инвентаря",
                () -> BurmaldaSettings.inventoryUsageHud, v -> BurmaldaSettings.inventoryUsageHud = v);
    }

    private static void add(String name, Category category, String description,
                            BooleanSupplier getter, Consumer<Boolean> setter) {
        MODULES.add(new Module(name, category, description, getter, setter));
    }

    static List<Module> all() { return List.copyOf(MODULES); }

    private BurmaldaModules() {}
}
