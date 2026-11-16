package ru.burmalda.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class BurmaldaScreen extends Screen {
    public BurmaldaScreen() { super(Text.literal("Burmalda Client")); }

    @Override
    protected void init() {
        int centerX = width / 2, centerY = height / 2;
        addDrawableChild(ButtonWidget.builder(
                Text.literal(BurmaldaClient.isHudVisible() ? "HUD: ON" : "HUD: OFF"),
                button -> {
                    boolean next = !BurmaldaClient.isHudVisible();
                    BurmaldaClient.setHudVisible(next);
                    button.setMessage(Text.literal(next ? "HUD: ON" : "HUD: OFF"));
                }).dimensions(centerX - 90, centerY - 10, 180, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> close())
                .dimensions(centerX - 90, centerY + 18, 180, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 45, 0xFF66CCFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() { return false; }
}
