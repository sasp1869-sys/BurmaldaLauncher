package ru.burmalda.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.registry.Registries;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class BurmaldaClient implements ClientModInitializer {
    private static KeyBinding openMenuKey;
    private static KeyBinding toggleHudKey;
    private static KeyBinding hudEditorKey;
    private static int hue;
    private static double previousGamma = -1.0;
    private static boolean previousAutoJump;
    private static boolean autoJumpWasEnabled;
    private static boolean autoWalkWasActive;

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

        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            if (!message.equals(".cfg") && !message.startsWith(".cfg ")) return true;
            handleConfigCommand(message);
            return false;
        });

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
            updateAutoWalk(client);
            updateAutoJump(client);
        });

        ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipType, lines) -> {
            if (!BurmaldaSettings.betterTooltips || stack.isEmpty()) return;
            lines.add(Text.literal("§8ID: " + Registries.ITEM.getId(stack.getItem())));
            if (stack.isDamageable()) {
                int remaining = Math.max(0, 100 - stack.getDamage() * 100 / stack.getMaxDamage());
                lines.add(Text.literal("§7Durability: §f" + remaining + "%"));
            }
            if (stack.getCount() > 1) {
                lines.add(Text.literal("§7Stack: §f" + stack.getCount()));
            }
        });

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> renderHud(drawContext));
    }

    private static void handleConfigCommand(String raw) {
        MinecraftClient client = MinecraftClient.getInstance();
        String[] parts = raw.trim().split("\\s+");
        if (client.player == null) return;

        if (parts.length == 1 || parts[1].equalsIgnoreCase("help")) {
            configMessage(".cfg save <name> | .cfg load <name> | .cfg list | .cfg delete <name>");
            return;
        }

        String action = parts[1].toLowerCase(java.util.Locale.ROOT);
        if (action.equals("list")) {
            List<String> configs = BurmaldaSettings.listNamed();
            configMessage(configs.isEmpty() ? "Сохранённых конфигов пока нет." : "Конфиги: " + String.join(", ", configs));
            return;
        }

        if (parts.length < 3) {
            configMessage("Укажи имя: .cfg " + action + " <name>");
            return;
        }

        String name = parts[2];
        switch (action) {
            case "save" -> configMessage(BurmaldaSettings.saveNamed(name)
                    ? "Конфиг '" + name + "' сохранён."
                    : "Не удалось сохранить. Имя: латиница, цифры, _ или -, до 32 символов.");
            case "load" -> configMessage(BurmaldaSettings.loadNamed(name)
                    ? "Конфиг '" + name + "' загружен."
                    : "Конфиг не найден или имя некорректно.");
            case "delete" -> configMessage(BurmaldaSettings.deleteNamed(name)
                    ? "Конфиг '" + name + "' удалён."
                    : "Не удалось удалить конфиг.");
            default -> configMessage("Команда не найдена. Напиши .cfg help");
        }
    }

    private static void configMessage(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) client.player.sendMessage(Text.literal("§b[Burmalda] §f" + message), false);
    }

    private static void updateSprint(MinecraftClient client) {
        if (client.player == null || !BurmaldaSettings.sprint) return;
        if (client.player.input.movementForward > 0.0F && client.player.getHungerManager().getFoodLevel() > 6) {
            client.player.setSprinting(true);
        }
    }

    private static void updateAutoWalk(MinecraftClient client) {
        boolean shouldWalk = BurmaldaSettings.autoWalk && client.player != null && client.currentScreen == null;
        if (shouldWalk) {
            client.options.forwardKey.setPressed(true);
        } else if (autoWalkWasActive) {
            client.options.forwardKey.setPressed(false);
        }
        autoWalkWasActive = shouldWalk;
    }

    private static void updateAutoJump(MinecraftClient client) {
        if (BurmaldaSettings.autoJump) {
            if (!previousAutoJump) {
                autoJumpWasEnabled = client.options.getAutoJump().getValue();
                previousAutoJump = true;
            }
            if (!client.options.getAutoJump().getValue()) client.options.getAutoJump().setValue(true);
        } else if (previousAutoJump) {
            client.options.getAutoJump().setValue(autoJumpWasEnabled);
            previousAutoJump = false;
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
                + (BurmaldaSettings.coordinates ? 1 : 0)
                + (BurmaldaSettings.pingHud ? 1 : 0)
                + (BurmaldaSettings.directionHud ? 1 : 0)
                + (BurmaldaSettings.speedHud ? 1 : 0)
                + (BurmaldaSettings.biomeHud ? 1 : 0)
                + (BurmaldaSettings.armorHud ? 1 : 0)
                + (BurmaldaSettings.keystrokesHud ? 2 : 0)
                + (BurmaldaSettings.dimensionHud ? 1 : 0)
                + (BurmaldaSettings.lightHud ? 1 : 0)
                + (BurmaldaSettings.clockHud ? 1 : 0);
        if (lines == 0 && !BurmaldaSettings.itemHighlight) return;

        int lineHeight = (BurmaldaSettings.compact || BurmaldaSettings.performance) ? 10 : 12;
        int width = (BurmaldaSettings.compact || BurmaldaSettings.performance) ? 150 : 196;
        if (BurmaldaSettings.panel) {
            context.fill(x - 4, y - 4, x + width, y + lines * lineHeight + 3, 0x990B1020);
            context.fill(x - 4, y - 4, x + width, y - 2, titleColor);
        }

        if (BurmaldaSettings.itemHighlight) {
            var heldStack = client.player.getMainHandStack();
            if (!heldStack.isEmpty()) {
                int itemX = client.getWindow().getScaledWidth() / 2 - 90;
                int itemY = client.getWindow().getScaledHeight() - 54;
                context.fill(itemX - 5, itemY - 4, itemX + 150, itemY + 22, 0x990B1020);
                context.drawItem(heldStack, itemX, itemY);
                String heldName = heldStack.getName().getString();
                String details = heldName + (heldStack.getCount() > 1 ? " x" + heldStack.getCount() : "");
                if (heldStack.isDamageable()) {
                    int remaining = Math.max(0, 100 - heldStack.getDamage() * 100 / heldStack.getMaxDamage());
                    details += "  " + remaining + "%";
                }
                context.drawTextWithShadow(client.textRenderer, Text.literal(details), itemX + 22, itemY + 6, 0xFFFFFFFF);
            }
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
            currentY += lineHeight;
        }
        if (BurmaldaSettings.pingHud) {
            var entry = client.getNetworkHandler() == null ? null : client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
            String ping = entry == null ? "Ping: --" : "Ping: " + entry.getLatency() + " ms";
            context.drawTextWithShadow(client.textRenderer, Text.literal(ping), x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.directionHud) {
            float yaw = client.player.getYaw();
            String direction = yaw >= -45 && yaw < 45 ? "South" : yaw >= 45 && yaw < 135 ? "West" : yaw >= -135 && yaw < -45 ? "East" : "North";
            context.drawTextWithShadow(client.textRenderer, Text.literal("Facing: " + direction), x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.speedHud) {
            double vx = client.player.getVelocity().x;
            double vz = client.player.getVelocity().z;
            double speed = Math.sqrt(vx * vx + vz * vz) * 20.0;
            context.drawTextWithShadow(client.textRenderer, Text.literal(String.format(java.util.Locale.ROOT, "Speed: %.2f b/s", speed)), x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.biomeHud && client.world != null) {
            String biome = client.world.getBiome(client.player.getBlockPos()).getKey()
                    .map(key -> key.getValue().getPath()).orElse("unknown");
            context.drawTextWithShadow(client.textRenderer, Text.literal("Biome: " + biome), x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.armorHud) {
            StringBuilder armor = new StringBuilder("Armor: ");
            boolean first = true;
            for (var stack : client.player.getArmorItems()) {
                if (stack.isEmpty()) continue;
                if (!first) armor.append(" | ");
                first = false;
                int remaining = stack.isDamageable()
                        ? Math.max(0, 100 - stack.getDamage() * 100 / stack.getMaxDamage()) : 100;
                armor.append(stack.getName().getString()).append(" ").append(remaining).append("%");
            }
            if (first) armor.append("empty");
            context.drawTextWithShadow(client.textRenderer, Text.literal(armor.toString()), x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.dimensionHud && client.world != null) {
            String dimension = client.world.getRegistryKey().getValue().getPath();
            context.drawTextWithShadow(client.textRenderer, Text.literal("Dimension: " + dimension), x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.lightHud && client.world != null) {
            int light = client.world.getLightLevel(client.player.getBlockPos());
            context.drawTextWithShadow(client.textRenderer, Text.literal("Light: " + light), x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.clockHud && client.world != null) {
            long time = client.world.getTimeOfDay() % 24000L;
            long hours = (time / 1000L + 6L) % 24L;
            long minutes = time % 1000L * 60L / 1000L;
            context.drawTextWithShadow(client.textRenderer,
                    Text.literal(String.format(java.util.Locale.ROOT, "World time: %02d:%02d", hours, minutes)),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.keystrokesHud) {
            String keys = (client.options.forwardKey.isPressed() ? "[W]" : " W ")
                    + (client.options.leftKey.isPressed() ? "[A]" : " A ")
                    + (client.options.backKey.isPressed() ? "[S]" : " S ")
                    + (client.options.rightKey.isPressed() ? "[D]" : " D ");
            context.drawTextWithShadow(client.textRenderer, Text.literal(keys), x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
            String actions = (client.options.jumpKey.isPressed() ? "[SPACE]" : " SPACE ")
                    + (client.options.sprintKey.isPressed() ? "[SPRINT]" : " SPRINT ");
            context.drawTextWithShadow(client.textRenderer, Text.literal(actions), x, currentY, 0xFFFFFFFF);
        }
    }

    static void moveHud(int dx, int dy) {
        BurmaldaSettings.hudX = Math.max(0, Math.min(1000, BurmaldaSettings.hudX + dx));
        BurmaldaSettings.hudY = Math.max(0, Math.min(700, BurmaldaSettings.hudY + dy));
        BurmaldaSettings.save();
    }

    public BurmaldaClient() {}
}
