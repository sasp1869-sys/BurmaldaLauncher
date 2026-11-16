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
    private static final int SIDEBAR_WIDTH = 132;
    private static final int ROW_HEIGHT = 32;
    private static final int ROW_GAP = 4;

    private final boolean editor;
    private BurmaldaModules.Category selectedCategory;
    private BurmaldaModules.Module selectedModule;
    private TextFieldWidget searchField;
    private int moduleScroll;

    private int panelX, panelY, panelWidth, panelHeight;
    private int listX, listWidth, detailsX, detailsWidth, listTop, listBottom;

    public BurmaldaScreen() { this(false, null); }
    public BurmaldaScreen(boolean editor) { this(editor, null); }

    private BurmaldaScreen(boolean editor, BurmaldaModules.Category category) {
        super(Text.literal(editor ? "Burmalda HUD Editor" : "Burmalda Client"));
        this.editor = editor;
        this.selectedCategory = category;
    }

    private void calculateLayout() {
        panelWidth = Math.min(900, width - 20);
        panelHeight = Math.min(500, height - 20);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        listX = panelX + SIDEBAR_WIDTH + 14;
        detailsWidth = Math.min(220, Math.max(170, panelWidth / 4));
        detailsX = panelX + panelWidth - detailsWidth - 12;
        listWidth = detailsX - listX - 12;
        listTop = panelY + 91;
        listBottom = panelY + panelHeight - 34;
    }

    @Override
    protected void init() {
        calculateLayout();
        if (editor) {
            int cx = width / 2;
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
            }).dimensions(cx - 152, top + 72, 304, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Назад"), b -> client.setScreen(new BurmaldaScreen()))
                    .dimensions(cx - 152, top + 96, 304, 20).build());
            return;
        }

        searchField = new TextFieldWidget(textRenderer, listX, panelY + 59,
                listWidth, 23, Text.literal("Search modules"));
        searchField.setMaxLength(48);
        searchField.setPlaceholder(Text.literal("Поиск модулей..."));
        searchField.setDrawsBackground(false);
        addDrawableChild(searchField);
    }

    private List<BurmaldaModules.Module> visibleModules() {
        String query = searchField == null ? "" : searchField.getText().toLowerCase(Locale.ROOT).trim();
        return BurmaldaModules.all().stream()
                .filter(module -> selectedCategory == null || module.category == selectedCategory)
                .filter(module -> query.isEmpty()
                        || module.name.toLowerCase(Locale.ROOT).contains(query)
                        || module.description.toLowerCase(Locale.ROOT).contains(query))
                .collect(Collectors.toList());
    }

    private int visibleRows() {
        return Math.max(1, (listBottom - listTop) / (ROW_HEIGHT + ROW_GAP));
    }

    private int maxScroll(List<BurmaldaModules.Module> modules) {
        return Math.max(0, modules.size() - visibleRows());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!editor && button == 0
                && mouseX >= panelX + 9 && mouseX < panelX + SIDEBAR_WIDTH - 8
                && mouseY >= panelY + 55 && mouseY < panelY + panelHeight - 38) {
            BurmaldaModules.Category[] categories = BurmaldaModules.Category.values();
            int index = (int) ((mouseY - (panelY + 55)) / 21);
            if (index >= 0 && index <= categories.length) {
                selectedCategory = index == 0 ? null : categories[index - 1];
                selectedModule = null;
                moduleScroll = 0;
                return true;
            }
        }

        if (!editor && button == 0 && mouseX >= listX && mouseX < listX + listWidth
                && mouseY >= listTop && mouseY < listBottom) {
            List<BurmaldaModules.Module> modules = visibleModules();
            int row = (int) ((mouseY - listTop) / (ROW_HEIGHT + ROW_GAP)) + moduleScroll;
            if (row >= 0 && row < modules.size()) {
                BurmaldaModules.Module module = modules.get(row);
                int rowY = listTop + (row - moduleScroll) * (ROW_HEIGHT + ROW_GAP);
                if (mouseX >= listX + listWidth - 46 && mouseY <= rowY + ROW_HEIGHT) {
                    module.toggle();
                    selectedModule = module;
                } else {
                    selectedModule = module;
                }
                return true;
            }
        }

        if (!editor && button == 0 && selectedModule != null
                && mouseX >= detailsX + 12 && mouseX <= detailsX + detailsWidth - 12
                && mouseY >= panelY + 166 && mouseY <= panelY + 194) {
            selectedModule.toggle();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!editor && mouseX >= listX && mouseX <= listX + listWidth
                && mouseY >= listTop && mouseY <= listBottom) {
            moduleScroll = Math.max(0, Math.min(maxScroll(visibleModules()),
                    moduleScroll - (int) Math.signum(verticalAmount)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void drawText(DrawContext context, String value, int x, int y, int color) {
        context.drawText(textRenderer, Text.literal(value), x, y, color, false);
    }

    private void drawCategory(DrawContext context, String label, int index, boolean active,
                              int mouseX, int mouseY) {
        int x = panelX + 9;
        int y = panelY + 55 + index * 21;
        int right = panelX + SIDEBAR_WIDTH - 8;
        boolean hovered = mouseX >= x && mouseX <= right && mouseY >= y && mouseY < y + 18;
        if (active) {
            context.fill(x, y, right, y + 18, 0xFF263D5B);
            context.fill(x, y + 3, x + 3, y + 15, 0xFF65B7FF);
        } else if (hovered) {
            context.fill(x, y, right, y + 18, 0xFF202A3A);
        }
        drawText(context, label, x + 9, y + 5, active ? 0xFFFFFFFF : 0xFFADB8C8);
    }

    private void drawModuleRow(DrawContext context, BurmaldaModules.Module module,
                               int x, int y, int mouseX, int mouseY) {
        boolean enabled = module.enabled();
        boolean selected = module == selectedModule;
        boolean hovered = mouseX >= x && mouseX <= x + listWidth
                && mouseY >= y && mouseY <= y + ROW_HEIGHT;
        context.fill(x, y, x + listWidth, y + ROW_HEIGHT,
                selected ? 0xFF26364B : hovered ? 0xFF202C3E : 0xFF172232);
        if (selected) context.fill(x, y, x + 2, y + ROW_HEIGHT, 0xFF65B7FF);
        drawText(context, textRenderer.trimToWidth(module.name, listWidth - 66),
                x + 10, y + 5, enabled ? 0xFFF3F7FC : 0xFFD4DCE7);
        drawText(context, textRenderer.trimToWidth(module.description, listWidth - 66),
                x + 10, y + 18, 0xFF8392A6);

        int sx = x + listWidth - 39;
        context.fill(sx, y + 8, sx + 29, y + 23, enabled ? 0xFF287D68 : 0xFF354255);
        int knobX = enabled ? sx + 16 : sx + 2;
        context.fill(knobX, y + 10, knobX + 11, y + 21, 0xFFF1F5FA);
    }

    private void drawDetails(DrawContext context, int mouseX, int mouseY) {
        int x = detailsX;
        int y = panelY + 51;
        context.fill(x, y, x + detailsWidth, panelY + panelHeight - 35, 0xFF111B2A);
        drawText(context, "MODULE DETAILS", x + 12, y + 13, 0xFF7187A3);
        if (selectedModule == null) {
            drawText(context, "Select a module", x + 12, y + 42, 0xFFE8EEF6);
            drawText(context, "Click a module row to", x + 12, y + 61, 0xFF91A0B3);
            drawText(context, "see its description.", x + 12, y + 74, 0xFF91A0B3);
            drawText(context, "Use the switch to", x + 12, y + 103, 0xFF91A0B3);
            drawText(context, "enable or disable it.", x + 12, y + 116, 0xFF91A0B3);
            return;
        }

        drawText(context, textRenderer.trimToWidth(selectedModule.name, detailsWidth - 24),
                x + 12, y + 42, 0xFFF2F6FC);
        drawText(context, selectedModule.category.title.toUpperCase(Locale.ROOT),
                x + 12, y + 61, 0xFF73BFFF);

        String description = selectedModule.description;
        int lineY = y + 91;
        int maxChars = Math.max(18, (detailsWidth - 24) / 6);
        for (int start = 0; start < description.length() && lineY < y + 145; start += maxChars) {
            drawText(context, description.substring(start, Math.min(description.length(), start + maxChars)),
                    x + 12, lineY, 0xFF9BA9BB);
            lineY += 13;
        }

        int bx = x + 12;
        int by = panelY + 166;
        boolean enabled = selectedModule.enabled();
        boolean hovered = mouseX >= bx && mouseX <= x + detailsWidth - 12
                && mouseY >= by && mouseY <= by + 28;
        context.fill(bx, by, x + detailsWidth - 12, by + 28,
                enabled ? (hovered ? 0xFF32977C : 0xFF287D68)
                        : (hovered ? 0xFF40516A : 0xFF303E52));
        String action = enabled ? "ENABLED  •  CLICK TO DISABLE" : "DISABLED  •  CLICK TO ENABLE";
        Text actionText = Text.literal(action);
        context.drawText(textRenderer, actionText,
                x + (detailsWidth - textRenderer.getWidth(actionText)) / 2, by + 10, 0xFFFFFFFF, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Avoid Minecraft background-blur post-processing; keep the menu and text crisp.
        context.fill(0, 0, width, height, 0xB8000000);
        if (editor) {
            context.fill(width / 2 - 170, height / 2 - 100, width / 2 + 170, height / 2 + 130, 0xEE111927);
            Text editorTitle = title;
            context.drawText(textRenderer, editorTitle, width / 2 - textRenderer.getWidth(editorTitle) / 2,
                    height / 2 - 88, 0xFF72C5FF, false);
            Text editorHint = Text.literal("Перемещай HUD стрелками. F6 — открыть редактор.");
            context.drawText(textRenderer, editorHint,
                    width / 2 - textRenderer.getWidth(editorHint) / 2, height / 2 - 72, 0xFFCCD5E2, false);
            super.render(context, mouseX, mouseY, delta);
            return;
        }

        calculateLayout();
        context.fill(panelX - 1, panelY - 1, panelX + panelWidth + 1, panelY + panelHeight + 1, 0xFF35445A);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xF20D1420);
        context.fill(panelX, panelY, panelX + SIDEBAR_WIDTH, panelY + panelHeight, 0xFF111B2A);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + 2, 0xFF64B8FF);
        context.fill(panelX + SIDEBAR_WIDTH, panelY + 44, panelX + panelWidth, panelY + 45, 0xFF263246);

        drawText(context, "BURMALDA", panelX + 14, panelY + 15, 0xFF76C7FF);
        drawText(context, "CLIENT  /  1.21.1", panelX + 14, panelY + 29, 0xFF73849A);
        drawText(context, "CATEGORIES", panelX + 12, panelY + 43, 0xFF667A94);

        BurmaldaModules.Category[] categories = BurmaldaModules.Category.values();
        drawCategory(context, "All modules", 0, selectedCategory == null, mouseX, mouseY);
        for (int i = 0; i < categories.length; i++) {
            drawCategory(context, categories[i].title, i + 1,
                    categories[i] == selectedCategory, mouseX, mouseY);
        }

        String heading = selectedCategory == null ? "All modules" : selectedCategory.title;
        drawText(context, heading, listX, panelY + 16, 0xFFF0F5FC);
        drawText(context, "MODULES  /  RIGHT SHIFT TO CLOSE", listX, panelY + 32, 0xFF8291A5);
        context.fill(listX, panelY + 56, listX + listWidth, panelY + 84, 0xFF172232);
        context.fill(detailsX, panelY + 43, detailsX + detailsWidth, panelY + 44, 0xFF263246);

        List<BurmaldaModules.Module> modules = visibleModules();
        moduleScroll = Math.min(moduleScroll, maxScroll(modules));
        context.enableScissor(listX, listTop, listX + listWidth, listBottom);
        for (int row = moduleScroll; row < Math.min(modules.size(), moduleScroll + visibleRows()); row++) {
            int y = listTop + (row - moduleScroll) * (ROW_HEIGHT + ROW_GAP);
            drawModuleRow(context, modules.get(row), listX, y, mouseX, mouseY);
        }
        context.disableScissor();

        if (modules.isEmpty()) {
            Text emptyText = Text.literal("No modules found");
            context.drawText(textRenderer, emptyText,
                    listX + (listWidth - textRenderer.getWidth(emptyText)) / 2, listTop + 20, 0xFF9BA9BB, false);
        }
        drawDetails(context, mouseX, mouseY);
        context.fill(listX, panelY + panelHeight - 27, panelX + panelWidth - 12,
                panelY + panelHeight - 26, 0xFF263246);
        drawText(context, modules.size() + " modules  •  Scroll to browse  •  Click row to inspect",
                listX, panelY + panelHeight - 18, 0xFF7F8FA4);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() { return false; }
}
