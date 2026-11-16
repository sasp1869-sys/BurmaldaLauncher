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
    private static final int SIDEBAR_WIDTH = 126;
    private static final int CARD_HEIGHT = 56;
    private static final int CARD_GAP = 8;
    private static final int ROW_STEP = CARD_HEIGHT + CARD_GAP;

    private final boolean editor;
    private BurmaldaModules.Category selectedCategory;
    private TextFieldWidget searchField;
    private int moduleScroll;

    private int panelX, panelY, panelWidth, panelHeight, contentX, contentWidth;
    private int cardsTop, cardsBottom, cardColumns, cardWidth;

    public BurmaldaScreen() { this(false, null); }
    public BurmaldaScreen(boolean editor) { this(editor, null); }

    private BurmaldaScreen(boolean editor, BurmaldaModules.Category category) {
        super(Text.literal(editor ? "Burmalda HUD Editor" : "Burmalda Client"));
        this.editor = editor;
        this.selectedCategory = category;
    }

    private void calculateLayout() {
        panelWidth = Math.min(660, width - 24);
        panelHeight = Math.min(440, height - 24);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        contentX = panelX + SIDEBAR_WIDTH + 18;
        contentWidth = panelWidth - SIDEBAR_WIDTH - 32;
        cardsTop = panelY + 104;
        cardsBottom = panelY + panelHeight - 38;
        cardColumns = contentWidth >= 390 ? 2 : 1;
        cardWidth = (contentWidth - (cardColumns - 1) * CARD_GAP) / cardColumns;
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

        searchField = new TextFieldWidget(textRenderer, contentX, panelY + 67,
                contentWidth, 22, Text.literal("Search modules"));
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
        return Math.max(1, (cardsBottom - cardsTop) / ROW_STEP);
    }

    private int maxScroll(List<BurmaldaModules.Module> modules) {
        int rows = (modules.size() + cardColumns - 1) / cardColumns;
        return Math.max(0, rows - visibleRows());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!editor && button == 0 && mouseX >= panelX + 10 && mouseX < panelX + SIDEBAR_WIDTH - 8
                && mouseY >= panelY + 48 && mouseY < panelY + panelHeight - 40) {
            BurmaldaModules.Category[] categories = BurmaldaModules.Category.values();
            int index = (int) ((mouseY - (panelY + 48)) / 21);
            if (index >= 0 && index < categories.length) {
                selectedCategory = index == 0 ? null : categories[index];
                moduleScroll = 0;
                return true;
            }
        }

        if (!editor && button == 0 && mouseX >= contentX && mouseX < contentX + contentWidth
                && mouseY >= cardsTop && mouseY < cardsBottom) {
            List<BurmaldaModules.Module> modules = visibleModules();
            int col = Math.min(cardColumns - 1, Math.max(0, (int) ((mouseX - contentX) / (cardWidth + CARD_GAP))));
            int row = (int) ((mouseY - cardsTop) / ROW_STEP) + moduleScroll;
            int index = row * cardColumns + col;
            int localX = contentX + col * (cardWidth + CARD_GAP);
            int localY = cardsTop + (row - moduleScroll) * ROW_STEP;
            if (mouseX >= localX && mouseX <= localX + cardWidth
                    && mouseY >= localY && mouseY <= localY + CARD_HEIGHT
                    && index >= 0 && index < modules.size()) {
                modules.get(index).toggle();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!editor && mouseX >= contentX && mouseY >= cardsTop && mouseY <= cardsBottom) {
            moduleScroll = Math.max(0, Math.min(maxScroll(visibleModules()),
                    moduleScroll - (int) Math.signum(verticalAmount)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void drawCategory(DrawContext context, BurmaldaModules.Category category,
                              int index, int mouseX, int mouseY) {
        int x = panelX + 10;
        int y = panelY + 48 + index * 21;
        int right = panelX + SIDEBAR_WIDTH - 8;
        boolean active = category == selectedCategory || (index == 0 && selectedCategory == null);
        boolean hovered = mouseX >= x && mouseX <= right && mouseY >= y && mouseY < y + 18;
        if (active) {
            context.fill(x, y, right, y + 18, 0xFF263D5B);
            context.fill(x, y + 3, x + 3, y + 15, 0xFF65B7FF);
        } else if (hovered) {
            context.fill(x, y, right, y + 18, 0xFF202A3A);
        }
        String label = index == 0 ? "All modules" : category.title;
        context.drawTextWithShadow(textRenderer, Text.literal(label), x + 9, y + 5,
                active ? 0xFFFFFFFF : 0xFFADB8C8);
    }

    private void drawModuleCard(DrawContext context, BurmaldaModules.Module module,
                                int x, int y, int mouseX, int mouseY) {
        boolean enabled = module.enabled();
        boolean hovered = mouseX >= x && mouseX <= x + cardWidth
                && mouseY >= y && mouseY <= y + CARD_HEIGHT;
        int background = hovered ? 0xFF202C3E : 0xFF192333;
        context.fill(x, y, x + cardWidth, y + CARD_HEIGHT, background);
        context.fill(x, y, x + 2, y + CARD_HEIGHT, enabled ? 0xFF55D6A0 : 0xFF354255);

        int switchWidth = 29;
        int switchX = x + cardWidth - switchWidth - 10;
        int switchY = y + 12;
        context.fill(switchX, switchY, switchX + switchWidth, switchY + 15,
                enabled ? 0xFF287D68 : 0xFF354255);
        int knobX = enabled ? switchX + 16 : switchX + 2;
        context.fill(knobX, switchY + 2, knobX + 11, switchY + 13, 0xFFF1F5FA);

        int textMax = Math.max(60, switchX - x - 18);
        context.drawTextWithShadow(textRenderer,
                Text.literal(textRenderer.trimToWidth(module.name, textMax)),
                x + 11, y + 9, enabled ? 0xFFFFFFFF : 0xFFE0E6EF);
        String description = textRenderer.trimToWidth(module.description, cardWidth - 22);
        context.drawText(textRenderer, description, x + 11, y + 29, 0xFF8F9CAF, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        if (editor) {
            context.fill(width / 2 - 170, height / 2 - 100, width / 2 + 170, height / 2 + 130, 0xEE111927);
            context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 88, 0xFF72C5FF);
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("Перемещай HUD стрелками. F6 — открыть редактор."),
                    width / 2, height / 2 - 72, 0xFFCCD5E2);
            super.render(context, mouseX, mouseY, delta);
            return;
        }

        calculateLayout();
        context.fill(panelX - 1, panelY - 1, panelX + panelWidth + 1, panelY + panelHeight + 1, 0xFF35445A);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xF20D1420);
        context.fill(panelX, panelY, panelX + SIDEBAR_WIDTH, panelY + panelHeight, 0xFF111B2A);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + 2, 0xFF64B8FF);
        context.fill(panelX + SIDEBAR_WIDTH, panelY + 43, panelX + panelWidth, panelY + 44, 0xFF263246);

        context.drawTextWithShadow(textRenderer, Text.literal("BURMALDA"),
                panelX + 14, panelY + 15, 0xFF76C7FF);
        context.drawText(textRenderer, Text.literal("CLIENT  /  1.21.1"),
                panelX + 14, panelY + 29, 0xFF73849A, false);
        context.drawTextWithShadow(textRenderer, Text.literal("CATEGORIES"),
                panelX + 12, panelY + 38, 0xFF667A94);

        BurmaldaModules.Category[] categories = BurmaldaModules.Category.values();
        drawCategory(context, null, 0, mouseX, mouseY);
        for (int i = 0; i < categories.length; i++) {
            drawCategory(context, categories[i], i + 1, mouseX, mouseY);
        }

        String heading = selectedCategory == null ? "All modules" : selectedCategory.title;
        context.drawTextWithShadow(textRenderer, Text.literal(heading),
                contentX, panelY + 16, 0xFFF0F5FC);
        context.drawText(textRenderer, Text.literal("Manage your client features"),
                contentX, panelY + 31, 0xFF8291A5, false);
        context.fill(contentX, panelY + 64, contentX + contentWidth, panelY + 91, 0xFF172232);
        context.drawText(textRenderer, Text.literal("⌕"), contentX + 8, panelY + 74, 0xFF7F91A8, false);

        List<BurmaldaModules.Module> modules = visibleModules();
        moduleScroll = Math.min(moduleScroll, maxScroll(modules));
        context.enableScissor(contentX, cardsTop, contentX + contentWidth, cardsBottom);
        for (int row = moduleScroll; row < moduleScroll + visibleRows(); row++) {
            for (int col = 0; col < cardColumns; col++) {
                int index = row * cardColumns + col;
                if (index >= modules.size()) break;
                int x = contentX + col * (cardWidth + CARD_GAP);
                int y = cardsTop + (row - moduleScroll) * ROW_STEP;
                drawModuleCard(context, modules.get(index), x, y, mouseX, mouseY);
            }
        }
        context.disableScissor();

        if (modules.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("No modules found"),
                    contentX + contentWidth / 2, cardsTop + 20, 0xFF9BA9BB);
        }
        context.fill(contentX, panelY + panelHeight - 29, panelX + panelWidth - 12,
                panelY + panelHeight - 28, 0xFF263246);
        context.drawText(textRenderer, Text.literal(modules.size() + " modules  •  Click a card to toggle  •  Scroll to browse"),
                contentX, panelY + panelHeight - 19, 0xFF7F8FA4, false);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() { return false; }
}
