package ru.burmalda.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class BurmaldaClient implements ClientModInitializer {
    private static KeyBinding openMenuKey;
    private static KeyBinding toggleHudKey;
    private static KeyBinding hudEditorKey;
    private static int hue;
    private static double previousGamma = -1.0;

    @Override
    public void onInitializeClient() {
        BurmaldaSettings.load();

        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.burmalda.open_menu", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT, "category.burmalda"));
        toggleHudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.burmalda.toggle_hud", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F7, "category.burmalda"));
        hudEditorKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.burmalda.hud_editor", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F6, "category.burmalda"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new BurmaldaScreen());
            }
            while (toggleHudKey.wasPressed()) {
                BurmaldaSettings.hud = !BurmaldaSettings.hud;
                BurmaldaSettings.save();
            }
            while (hudEditorKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new BurmaldaScreen(true));
            }
            if (BurmaldaSettings.rgb) hue = (hue + BurmaldaSettings.hueSpeed) % 360;
            updateFullbright(client);
            updateSprint(client);
        });

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> renderHud(drawContext));
    }

    private static void updateSprint(MinecraftClient client) {
        if (client.player == null || !BurmaldaSettings.sprint) return;
        if (client.player.input.movementForward > 0.0F && client.player.getHungerManager().getFoodLevel() > 6) {
            client.player.setSprinting(true);
        }
    }

    private static void updateFullbright(MinecraftClient client) {
        if (BurmaldaSettings.fullbright) {
            if (previousGamma < 0.0) previousGamma = client.options.getGamma().getValue();
            if (client.options.getGamma().getValue() < 16.0) client.options.getGamma().setValue(16.0);
        } else if (previousGamma >= 0.0) {
            client.options.getGamma().setValue(previousGamma);
            previousGamma = -1.0;
        }
    }

    private static void renderHud(net.minecraft.client.gui.DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!BurmaldaSettings.hud || client.player == null) return;

        int x = Math.max(0, BurmaldaSettings.hudX);
        int y = Math.max(0, BurmaldaSettings.hudY);
        int titleColor = BurmaldaSettings.rgb
                ? java.awt.Color.HSBtoRGB(hue / 360.0f, 0.85f, 1.0f) | 0xFF000000
                : 0xFF66CCFF;

        int lines = (BurmaldaSettings.watermark ? 1 : 0)
                + (BurmaldaSettings.fps ? 1 : 0)
                + (BurmaldaSettings.coordinates ? 1 : 0);
        if (lines == 0) return;

        int lineHeight = (BurmaldaSettings.compact || BurmaldaSettings.performance) ? 10 : 12;
        int width = (BurmaldaSettings.compact || BurmaldaSettings.performance) ? 126 : 164;
        if (BurmaldaSettings.panel) {
            context.fill(x - 4, y - 4, x + width, y + lines * lineHeight + 3, 0x990B1020);
            context.fill(x - 4, y - 4, x + width, y - 2, titleColor);
        }

        int currentY = y;
        if (BurmaldaSettings.watermark) {
            context.drawTextWithShadow(client.textRenderer, Text.literal("BURMALDA CLIENT"),
                    x, currentY, titleColor);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.fps) {
            context.drawTextWithShadow(client.textRenderer,
                    Text.literal("FPS: " + client.getCurrentFps()), x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.coordinates) {
            String coords = String.format(java.util.Locale.ROOT, "XYZ: %.1f / %.1f / %.1f",
                    client.player.getX(), client.player.getY(), client.player.getZ());
            context.drawTextWithShadow(client.textRenderer, Text.literal(coords), x, currentY, 0xFFFFFFFF);
        }
    }

    static void moveHud(int dx, int dy) {
        BurmaldaSettings.hudX = Math.max(0, Math.min(1000, BurmaldaSettings.hudX + dx));
        BurmaldaSettings.hudY = Math.max(0, Math.min(700, BurmaldaSettings.hudY + dy));
        BurmaldaSettings.save();
    }

    private BurmaldaClient() {}
}
