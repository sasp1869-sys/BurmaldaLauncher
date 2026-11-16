package ru.burmalda.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class BurmaldaScreen extends Screen {
    private static final int CATEGORY_Y = 32;
    private static final int FILTER_Y = 98;
    private static final int MODULES_Y = 128;
    private static final int MODULE_ROW_HEIGHT = 22;
    private static final int MODULE_COLUMN_WIDTH = 106;
    private static final int MODULE_COLUMNS = 3;

    private final boolean editor;
    private final BurmaldaModules.Category selectedCategory;
    private TextFieldWidget searchField;
    private int moduleScroll;

    public BurmaldaScreen() { this(false, null); }
    public BurmaldaScreen(boolean editor) { this(editor, null); }

    private BurmaldaScreen(boolean editor, BurmaldaModules.Category category) {
        super(Text.literal(editor ? "Burmalda HUD Editor" : "Burmalda Client"));
        this.editor = editor;
        this.selectedCategory = category;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int left = cx - 154;

        if (editor) {
            int top = height / 2 - 58;
            addDrawableChild(ButtonWidget.builder(Text.literal("↑"), b -> BurmaldaClient.moveHud(0, -8))
                    .dimensions(cx - 24, top, 48, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("←"), b -> BurmaldaClient.moveHud(-8, 0))
                    .dimensions(cx - 78, top + 24, 48, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("→"), b -> BurmaldaClient.moveHud(8, 0))
                    .dimensions(cx + 30, top + 24, 48, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("↓"), b -> BurmaldaClient.moveHud(0, 8))
                    .dimensions(cx - 24, top + 48, 48, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Сбросить позицию"), b -> {
                BurmaldaSettings.hudX = 8;
                BurmaldaSettings.hudY = 8;
                BurmaldaSettings.save();
            }).dimensions(left, top + 72, 304, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Назад"), b -> client.setScreen(new BurmaldaScreen()))
                    .dimensions(left, top + 96, 304, 20).build());
            return;
        }

        BurmaldaModules.Category[] categories = BurmaldaModules.Category.values();
        int cols = 5;
        int buttonWidth = 60;
        int buttonGap = 3;
        int totalWidth = cols * buttonWidth + (cols - 1) * buttonGap;
        int startX = cx - totalWidth / 2;
        for (int i = 0; i < categories.length; i++) {
            BurmaldaModules.Category category = categories[i];
            int x = startX + (i % cols) * (buttonWidth + buttonGap);
            int y = CATEGORY_Y + (i / cols) * 21;
            addDrawableChild(ButtonWidget.builder(Text.literal(category.title), b ->
                    client.setScreen(new BurmaldaScreen(false, category)))
                    .dimensions(x, y, buttonWidth, 18).build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Все модули"), b -> client.setScreen(new BurmaldaScreen()))
                .dimensions(startX, FILTER_Y, 76, 20).build());
        searchField = new TextFieldWidget(textRenderer, startX + 80, FILTER_Y, 145, 20, Text.literal("Search"));
        searchField.setMaxLength(48);
        searchField.setPlaceholder(Text.literal("Поиск модулей..."));
        addDrawableChild(searchField);
        addDrawableChild(ButtonWidget.builder(Text.literal("Редактор HUD"), b -> client.setScreen(new BurmaldaScreen(true)))
                .dimensions(startX + 229, FILTER_Y, 86, 20).build());
    }

    private List<BurmaldaModules.Module> visibleModules() {
        String query = searchField == null ? "" : searchField.getText().toLowerCase(Locale.ROOT).trim();
        return BurmaldaModules.all().stream()
                .filter(m -> selectedCategory == null || m.category == selectedCategory)
                .filter(m -> query.isEmpty() || m.name.toLowerCase(Locale.ROOT).contains(query)
                        || m.description.toLowerCase(Locale.ROOT).contains(query))
                .collect(Collectors.toList());
    }

    private int visibleRows() {
        return Math.max(1, (height - MODULES_Y - 28) / MODULE_ROW_HEIGHT);
    }

    private int maxScroll(List<BurmaldaModules.Module> modules) {
        int rows = (modules.size() + MODULE_COLUMNS - 1) / MODULE_COLUMNS;
        return Math.max(0, rows - visibleRows());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!editor && button == 0 && mouseY >= MODULES_Y && mouseY < height - 24) {
            List<BurmaldaModules.Module> modules = visibleModules();
            int startX = width / 2 - 160;
            int col = Math.max(0, Math.min(MODULE_COLUMNS - 1, (int)((mouseX - startX) / MODULE_COLUMN_WIDTH)));
            int x = startX + col * MODULE_COLUMN_WIDTH;
            int row = (int) ((mouseY - MODULES_Y) / MODULE_ROW_HEIGHT) + moduleScroll;
            int index = row * MODULE_COLUMNS + col;
            if (mouseX >= x && mouseX <= x + 100 && index >= 0 && index < modules.size()) {
                modules.get(index).toggle();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!editor && mouseY >= MODULES_Y) {
            moduleScroll = Math.max(0, Math.min(maxScroll(visibleModules()),
                    moduleScroll - (int) Math.signum(verticalAmount)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        int accent = BurmaldaSettings.rgb
                ? java.awt.Color.HSBtoRGB((System.currentTimeMillis() % 5000) / 5000.0f, 0.8f, 1.0f) | 0xFF000000
                : 0xFF66CCFF;
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, accent);

        if (editor) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("Перемещай HUD стрелками. F6 — открыть редактор."),
                    width / 2, height / 2 - 82, 0xFFCCCCCC);
        } else {
            String categoryName = selectedCategory == null ? "Все модули" : selectedCategory.title;
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(categoryName),
                    width / 2, 120, 0xFFCCCCCC);

            List<BurmaldaModules.Module> modules = visibleModules();
            moduleScroll = Math.min(moduleScroll, maxScroll(modules));
            int startX = width / 2 - 160;
            int rows = visibleRows();
            context.enableScissor(startX, MODULES_Y, startX + 320, height - 24);
            for (int visibleRow = 0; visibleRow < rows; visibleRow++) {
                int row = visibleRow + moduleScroll;
                for (int col = 0; col < 2; col++) {
                    int index = row * MODULE_COLUMNS + col;
                    if (index >= modules.size()) continue;
                    BurmaldaModules.Module module = modules.get(index);
                    int x = startX + col * MODULE_COLUMN_WIDTH;
                    int y = MODULES_Y + visibleRow * MODULE_ROW_HEIGHT;
                    context.fill(x, y, x + 100, y + 19, module.enabled() ? 0xCC174D38 : 0xCC252A35);
                    context.drawTextWithShadow(textRenderer,
                            Text.literal(module.name + (module.enabled() ? "  ON" : "  OFF")),
                            x + 4, y + 3, module.enabled() ? 0xFF8CFFB0 : 0xFFE0E0E0);
                }
            }
            context.disableScissor();

            if (modules.isEmpty()) {
                context.drawCenteredTextWithShadow(textRenderer,
                        Text.literal("В этой категории пока нет реализованных модулей"),
                        width / 2, MODULES_Y + 12, 0xFFAAAAAA);
            }
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("3 колонки • Колёсико — прокрутка • Автосохранение"),
                    width / 2, height - 14, 0xFFAAAAAA);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() { return false; }
}
