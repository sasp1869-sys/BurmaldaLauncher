package ru.burmalda.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class BurmaldaScreen extends Screen {
    private final boolean editor;

    public BurmaldaScreen() { this(false); }

    public BurmaldaScreen(boolean editor) {
        super(Text.literal(editor ? "Burmalda HUD Editor" : "Burmalda Client"));
        this.editor = editor;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int top = height / 2 - (editor ? 58 : 82);
        int left = cx - 154;
        int right = cx + 4;
        int w = 150;
        int h = 20;
        int gap = 24;

        if (!editor) {
            addToggle("HUD", () -> BurmaldaSettings.hud, v -> BurmaldaSettings.hud = v, left, top, w, h);
            addToggle("FPS", () -> BurmaldaSettings.fps, v -> BurmaldaSettings.fps = v, right, top, w, h);
            addToggle("Координаты", () -> BurmaldaSettings.coordinates, v -> BurmaldaSettings.coordinates = v, left, top + gap, w, h);
            addToggle("Название клиента", () -> BurmaldaSettings.watermark, v -> BurmaldaSettings.watermark = v, right, top + gap, w, h);
            addToggle("Панель HUD", () -> BurmaldaSettings.panel, v -> BurmaldaSettings.panel = v, left, top + gap * 2, w, h);
            addToggle("RGB-анимация", () -> BurmaldaSettings.rgb, v -> BurmaldaSettings.rgb = v, right, top + gap * 2, w, h);
            addToggle("Компактный HUD", () -> BurmaldaSettings.compact, v -> BurmaldaSettings.compact = v, left, top + gap * 3, w, h);
            addToggle("Режим производительности", () -> BurmaldaSettings.performance, v -> BurmaldaSettings.performance = v, right, top + gap * 3, w, h);
            addDrawableChild(ButtonWidget.builder(Text.literal("Редактор HUD"), b -> client.setScreen(new BurmaldaScreen(true)))
                    .dimensions(left, top + gap * 4, 304, h).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Сохранить и закрыть"), b -> {
                BurmaldaSettings.save();
                close();
            }).dimensions(left, top + gap * 5, 304, h).build());
        } else {
            addDrawableChild(ButtonWidget.builder(Text.literal("↑"), b -> BurmaldaClient.moveHud(0, -8))
                    .dimensions(cx - 24, top, 48, h).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("←"), b -> BurmaldaClient.moveHud(-8, 0))
                    .dimensions(cx - 78, top + gap, 48, h).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("→"), b -> BurmaldaClient.moveHud(8, 0))
                    .dimensions(cx + 30, top + gap, 48, h).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("↓"), b -> BurmaldaClient.moveHud(0, 8))
                    .dimensions(cx - 24, top + gap * 2, 48, h).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Сбросить позицию"), b -> {
                BurmaldaSettings.hudX = 8;
                BurmaldaSettings.hudY = 8;
                BurmaldaSettings.save();
            }).dimensions(left, top + gap * 3, 304, h).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Назад"), b -> client.setScreen(new BurmaldaScreen()))
                    .dimensions(left, top + gap * 4, 304, h).build());
        }
    }

    private void addToggle(String name, java.util.function.BooleanSupplier getter,
                           java.util.function.Consumer<Boolean> setter,
                           int x, int y, int width, int height) {
        ButtonWidget button = ButtonWidget.builder(label(name, getter.getAsBoolean()), b -> {
            boolean next = !getter.getAsBoolean();
            setter.accept(next);
            BurmaldaSettings.save();
            b.setMessage(label(name, next));
        }).dimensions(x, y, width, height).build();
        addDrawableChild(button);
    }

    private Text label(String name, boolean enabled) {
        return Text.literal(name + ": " + (enabled ? "ON" : "OFF"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        int accent = BurmaldaSettings.rgb
                ? java.awt.Color.HSBtoRGB((System.currentTimeMillis() % 5000) / 5000.0f, 0.8f, 1.0f) | 0xFF000000
                : 0xFF66CCFF;
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 105, accent);
        if (editor) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("Перемещай HUD стрелками ниже. F6 — открыть редактор."),
                    width / 2, height / 2 - 82, 0xFFCCCCCC);
        } else {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("Right Shift — меню  |  F6 — редактор HUD  |  F7 — HUD вкл/выкл"),
                    width / 2, height / 2 - 105 + 14, 0xFFCCCCCC);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() { return false; }
}
