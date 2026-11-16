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
    private static boolean hudVisible = true;
    private static int hue = 0;

    @Override
    public void onInitializeClient() {
        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.burmalda.open_menu", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT, "category.burmalda"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new BurmaldaScreen());
            }
            hue = (hue + 2) % 360;
        });

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (!hudVisible || client.player == null) return;
            int color = java.awt.Color.HSBtoRGB(hue / 360.0f, 0.85f, 1.0f) | 0xFF000000;
            int x = 8, y = 8;
            drawContext.drawTextWithShadow(client.textRenderer, Text.literal("BURMALDA CLIENT"), x, y, color);
            y += 12;
            drawContext.drawTextWithShadow(client.textRenderer, Text.literal("FPS: " + client.getCurrentFps()), x, y, 0xFFFFFFFF);
            y += 12;
            drawContext.drawTextWithShadow(client.textRenderer,
                    Text.literal(String.format("XYZ: %.1f / %.1f / %.1f",
                            client.player.getX(), client.player.getY(), client.player.getZ())),
                    x, y, 0xFFFFFFFF);
        });
    }

    static void setHudVisible(boolean visible) { hudVisible = visible; }
    static boolean isHudVisible() { return hudVisible; }
}
