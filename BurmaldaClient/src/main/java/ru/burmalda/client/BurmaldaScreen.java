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
    private final boolean editor;
    private final BurmaldaModules.Category selectedCategory;
    private TextFieldWidget searchField;

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
        int w = 150;
        int h = 20;

        if (editor) {
            int top = height / 2 - 58;
            addDrawableChild(ButtonWidget.builder(Text.literal("↑"), b -> BurmaldaClient.moveHud(0, -8))
                    .dimensions(cx - 24, top, 48, h).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("←"), b -> BurmaldaClient.moveHud(-8, 0))
                    .dimensions(cx - 78, top + 24, 48, h).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("→"), b -> BurmaldaClient.moveHud(8, 0))
                    .dimensions(cx + 30, top + 24, 48, h).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("↓"), b -> BurmaldaClient.moveHud(0, 8))
                    .dimensions(cx - 24, top + 48, 48, h).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Сбросить позицию"), b -> {
                BurmaldaSettings.hudX = 8;
                BurmaldaSettings.hudY = 8;
                BurmaldaSettings.save();
            }).dimensions(left, top + 72, 304, h).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Назад"), b -> client.setScreen(new BurmaldaScreen()))
                    .dimensions(left, top + 96, 304, h).build());
            return;
        }

        BurmaldaModules.Category[] categories = BurmaldaModules.Category.values();
        int cols = 4;
        int buttonWidth = 74;
        int buttonGap = 4;
        int totalWidth = cols * buttonWidth + (cols - 1) * buttonGap;
        int startX = cx - totalWidth / 2;
        for (int i = 0; i < categories.length; i++) {
            BurmaldaModules.Category category = categories[i];
            int x = startX + (i % cols) * (buttonWidth + buttonGap);
            int y = 34 + (i / cols) * 21;
            addDrawableChild(ButtonWidget.builder(Text.literal(category.title), b ->
                    client.setScreen(new BurmaldaScreen(false, category)))
                    .dimensions(x, y, buttonWidth, 18).build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Все"), b -> client.setScreen(new BurmaldaScreen()))
                .dimensions(startX, 98, 60, 20).build());
        searchField = new TextFieldWidget(textRenderer, startX + 64, 98, 164, 20, Text.literal("Search"));
        searchField.setMaxLength(40);
        addDrawableChild(searchField);
        addDrawableChild(ButtonWidget.builder(Text.literal("Редактор HUD"), b -> client.setScreen(new BurmaldaScreen(true)))
                .dimensions(startX + 232, 98, 76, 20).build());
    }

    private List<BurmaldaModules.Module> visibleModules() {
        String query = searchField == null ? "" : searchField.getText().toLowerCase(Locale.ROOT).trim();
        return BurmaldaModules.all().stream()
                .filter(m -> selectedCategory == null || m.category == selectedCategory)
                .filter(m -> query.isEmpty() || m.name.toLowerCase(Locale.ROOT).contains(query)
                        || m.description.toLowerCase(Locale.ROOT).contains(query))
                .collect(Collectors.toList());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!editor && button == 0 && mouseY >= 126) {
            List<BurmaldaModules.Module> modules = visibleModules();
            int startX = width / 2 - 154;
            int rowHeight = 25;
            for (int i = 0; i < modules.size() && i < 12; i++) {
                int col = i % 2;
                int row = i / 2;
                int x = startX + col * 158;
                int y = 126 + row * rowHeight;
                if (mouseX >= x && mouseX <= x + 150 && mouseY >= y && mouseY <= y + 21) {
                    modules.get(i).toggle();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
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
                    width / 2, 116, 0xFFCCCCCC);
            List<BurmaldaModules.Module> modules = visibleModules();
            for (int i = 0; i < modules.size() && i < 12; i++) {
                BurmaldaModules.Module module = modules.get(i);
                int col = i % 2;
                int row = i / 2;
                int x = width / 2 - 154 + col * 158;
                int y = 126 + row * 25;
                context.fill(x, y, x + 150, y + 21, module.enabled() ? 0xCC174D38 : 0xCC252A35);
                context.drawTextWithShadow(textRenderer,
                        Text.literal(module.name + (module.enabled() ? "  ON" : "  OFF")),
                        x + 6, y + 3, module.enabled() ? 0xFF8CFFB0 : 0xFFE0E0E0);
            }
            if (modules.isEmpty()) {
                context.drawCenteredTextWithShadow(textRenderer,
                        Text.literal("В этой категории пока нет готовых модулей"),
                        width / 2, 145, 0xFFAAAAAA);
            }
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("Нажми на модуль, чтобы включить или выключить. Настройки сохраняются."),
                    width / 2, height - 14, 0xFFAAAAAA);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() { return false; }
}
