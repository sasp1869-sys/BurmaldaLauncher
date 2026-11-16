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
import net.minecraft.util.hit.BlockHitResult;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class BurmaldaClient implements ClientModInitializer {
    private static KeyBinding openMenuKey;
    private static KeyBinding toggleHudKey;
    private static KeyBinding hudEditorKey;
    private static KeyBinding copyCoordinatesKey;
    private static int hue;
    private static double previousGamma = -1.0;
    private static boolean previousAutoJump;
    private static boolean autoJumpWasEnabled;
    private static boolean autoWalkWasActive;
    private static boolean cameraTweaksWasEnabled;
    private static boolean previousBobView;

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
        copyCoordinatesKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.burmalda.copy_coordinates", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F8, "category.burmalda"));

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
            while (copyCoordinatesKey.wasPressed()) {
                if (BurmaldaSettings.copyCoordinates && client.player != null && client.world != null) {
                    var pos = client.player.getBlockPos();
                    String coordinates = pos.getX() + " " + pos.getY() + " " + pos.getZ();
                    GLFW.glfwSetClipboardString(client.getWindow().getHandle(), coordinates);
                    client.player.sendMessage(Text.literal("§b[Burmalda] §fКоординаты скопированы: " + coordinates), false);
                }
            }
            if (BurmaldaSettings.rgb) hue = (hue + BurmaldaSettings.hueSpeed) % 360;
            updateFullbright(client);
            updateSprint(client);
            updateAutoTool(client);
            updateAutoWalk(client);
            updateAutoJump(client);
            updateCameraTweaks(client);
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

    private static void updateAutoTool(MinecraftClient client) {
        if (!BurmaldaSettings.autoTool || client.player == null || client.world == null
                || client.currentScreen != null || !client.options.attackKey.isPressed()
                || !(client.crosshairTarget instanceof BlockHitResult hit)) return;

        var state = client.world.getBlockState(hit.getBlockPos());
        if (state.isAir()) return;

        int bestSlot = client.player.getInventory().selectedSlot;
        float bestSpeed = client.player.getInventory().getStack(bestSlot).getMiningSpeedMultiplier(state);
        for (int slot = 0; slot < 9; slot++) {
            float speed = client.player.getInventory().getStack(slot).getMiningSpeedMultiplier(state);
            if (speed > bestSpeed + 0.01F) {
                bestSpeed = speed;
                bestSlot = slot;
            }
        }
        if (bestSlot != client.player.getInventory().selectedSlot) {
            client.player.getInventory().selectedSlot = bestSlot;
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

    private static void updateCameraTweaks(MinecraftClient client) {
        if (BurmaldaSettings.cameraTweaks) {
            if (!cameraTweaksWasEnabled) {
                previousBobView = client.options.getBobView().getValue();
                cameraTweaksWasEnabled = true;
            }
            if (client.options.getBobView().getValue()) {
                client.options.getBobView().setValue(false);
            }
        } else if (cameraTweaksWasEnabled) {
            client.options.getBobView().setValue(previousBobView);
            cameraTweaksWasEnabled = false;
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
                + (BurmaldaSettings.buildHeightHud ? 1 : 0)
                + (BurmaldaSettings.pingHud ? 1 : 0)
                + (BurmaldaSettings.directionHud ? 1 : 0)
                + (BurmaldaSettings.speedHud ? 1 : 0)
                + (BurmaldaSettings.biomeHud ? 1 : 0)
                + (BurmaldaSettings.armorHud ? 1 : 0)
                + (BurmaldaSettings.keystrokesHud ? 2 : 0)
                + (BurmaldaSettings.dimensionHud ? 1 : 0)
                + (BurmaldaSettings.lightHud ? 1 : 0)
                + (BurmaldaSettings.clockHud ? 1 : 0)
                + (BurmaldaSettings.potionHud ? Math.max(1, client.player.getStatusEffects().size()) : 0)
                + (BurmaldaSettings.foodHud ? 1 : 0)
                + (BurmaldaSettings.totemCounter ? 1 : 0)
                + (BurmaldaSettings.experienceHud ? 1 : 0)
                + (BurmaldaSettings.heldItemHud ? 1 : 0)
                + (BurmaldaSettings.durabilityHud ? 1 : 0)
                + (BurmaldaSettings.chunkHud ? 1 : 0)
                + (BurmaldaSettings.weatherHud ? 1 : 0)
                + (BurmaldaSettings.dayCounterHud ? 1 : 0)
                + (BurmaldaSettings.localTimeHud ? 1 : 0)
                + (BurmaldaSettings.memoryHud ? 1 : 0)
                + (BurmaldaSettings.targetBlockHud ? 1 : 0)
                + (BurmaldaSettings.targetEntityHud ? 1 : 0)
                + (BurmaldaSettings.emptySlotsHud ? 1 : 0)
                + (BurmaldaSettings.selectedSlotHud ? 1 : 0)
                + (BurmaldaSettings.facingAngleHud ? 1 : 0)
                + (BurmaldaSettings.verticalSpeedHud ? 1 : 0)
                + (BurmaldaSettings.fallDistanceHud ? 1 : 0)
                + (BurmaldaSettings.airHud ? 1 : 0)
                + (BurmaldaSettings.nearbyPlayersHud ? 1 : 0)
                + (BurmaldaSettings.nearbyMobsHud ? 1 : 0)
                + (BurmaldaSettings.targetDistanceHud ? 1 : 0)
                + (BurmaldaSettings.spawnDistanceHud ? 1 : 0)
                + (BurmaldaSettings.healthHud ? 1 : 0)
                + (BurmaldaSettings.absorptionHud ? 1 : 0)
                + (BurmaldaSettings.offhandHud ? 1 : 0)
                + (BurmaldaSettings.arrowCounterHud ? 1 : 0)
                + (BurmaldaSettings.pearlCounterHud ? 1 : 0)
                + (BurmaldaSettings.appleCounterHud ? 1 : 0)
                + (BurmaldaSettings.shieldStatusHud ? 1 : 0)
                + (BurmaldaSettings.targetBlockPosHud ? 1 : 0)
                + (BurmaldaSettings.targetHealthHud ? 1 : 0)
                + (BurmaldaSettings.spawnCoordsHud ? 1 : 0)
                + (BurmaldaSettings.saturationHud ? 1 : 0)
                + (BurmaldaSettings.groundStatusHud ? 1 : 0)
                + (BurmaldaSettings.sprintStatusHud ? 1 : 0)
                + (BurmaldaSettings.swimmingStatusHud ? 1 : 0)
                + (BurmaldaSettings.elytraStatusHud ? 1 : 0)
                + (BurmaldaSettings.blockLightHud ? 1 : 0)
                + (BurmaldaSettings.skyLightHud ? 1 : 0)
                + (BurmaldaSettings.moonPhaseHud ? 1 : 0)
                + (BurmaldaSettings.biomeTemperatureHud ? 1 : 0)
                + (BurmaldaSettings.inventoryUsageHud ? 1 : 0);
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
        if (BurmaldaSettings.buildHeightHud && client.world != null) {
            int minY = client.world.getBottomY();
            int maxY = minY + client.world.getDimension().height() - 1;
            context.drawTextWithShadow(client.textRenderer,
                    Text.literal("Build height: " + (int) Math.floor(client.player.getY()) + " / " + maxY),
                    x, currentY, 0xFFFFFFFF);
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
        if (BurmaldaSettings.potionHud) {
            if (client.player.getStatusEffects().isEmpty()) {
                context.drawTextWithShadow(client.textRenderer, Text.literal("Effects: none"), x, currentY, 0xFFB8C5D6);
                currentY += lineHeight;
            } else {
                for (var effect : client.player.getStatusEffects()) {
                    String effectName = effect.getEffectType().value().getName().getString();
                    int amplifier = effect.getAmplifier() + 1;
                    int duration = effect.getDuration();
                    String time = duration == -1 ? "∞" : String.format(java.util.Locale.ROOT, "%d:%02d", duration / 1200, (duration / 20) % 60);
                    context.drawTextWithShadow(client.textRenderer,
                            Text.literal(effectName + " " + amplifier + "  " + time), x, currentY, 0xFFFFFFFF);
                    currentY += lineHeight;
                }
            }
        }
        if (BurmaldaSettings.foodHud) {
            int food = client.player.getHungerManager().getFoodLevel();
            float saturation = client.player.getHungerManager().getSaturationLevel();
            context.drawTextWithShadow(client.textRenderer,
                    Text.literal(String.format(java.util.Locale.ROOT, "Food: %d/20  •  Saturation: %.1f", food, saturation)),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.totemCounter) {
            int totems = 0;
            for (var stack : client.player.getInventory().main) {
                if (stack.isOf(net.minecraft.item.Items.TOTEM_OF_UNDYING)) totems += stack.getCount();
            }
            context.drawTextWithShadow(client.textRenderer,
                    Text.literal("Totems: " + totems), x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }

        if (BurmaldaSettings.experienceHud) {
            context.drawTextWithShadow(client.textRenderer,
                    Text.literal("XP: " + client.player.experienceLevel + "  •  " +
                            Math.round(client.player.experienceProgress * 100) + "%"),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.heldItemHud) {
            var stack = client.player.getMainHandStack();
            String item = stack.isEmpty() ? "empty" : stack.getName().getString()
                    + (stack.getCount() > 1 ? " x" + stack.getCount() : "");
            context.drawTextWithShadow(client.textRenderer, Text.literal("Held: " + item),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.durabilityHud) {
            var stack = client.player.getMainHandStack();
            String durability = !stack.isDamageable() ? "n/a"
                    : Math.max(0, 100 - stack.getDamage() * 100 / stack.getMaxDamage()) + "%";
            context.drawTextWithShadow(client.textRenderer, Text.literal("Durability: " + durability),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.chunkHud) {
            var pos = client.player.getBlockPos();
            context.drawTextWithShadow(client.textRenderer,
                    Text.literal("Chunk: " + (pos.getX() >> 4) + ", " + (pos.getZ() >> 4)
                            + "  •  Local: " + Math.floorMod(pos.getX(), 16) + ", " + Math.floorMod(pos.getZ(), 16)),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.weatherHud && client.world != null) {
            String weather = client.world.isThundering() ? "Thunderstorm"
                    : client.world.isRaining() ? "Rain" : "Clear";
            context.drawTextWithShadow(client.textRenderer, Text.literal("Weather: " + weather),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.dayCounterHud && client.world != null) {
            context.drawTextWithShadow(client.textRenderer,
                    Text.literal("Day: " + (client.world.getTimeOfDay() / 24000L + 1)),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.localTimeHud) {
            context.drawTextWithShadow(client.textRenderer,
                    Text.literal("Local time: " + java.time.LocalTime.now().format(
                            java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.memoryHud) {
            Runtime runtime = Runtime.getRuntime();
            long used = (runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L);
            long max = runtime.maxMemory() / (1024L * 1024L);
            context.drawTextWithShadow(client.textRenderer,
                    Text.literal("Memory: " + used + " / " + max + " MB"),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.targetBlockHud && client.world != null
                && client.crosshairTarget instanceof BlockHitResult blockHit) {
            var state = client.world.getBlockState(blockHit.getBlockPos());
            String blockId = net.minecraft.registry.Registries.BLOCK.getId(state.getBlock()).toString();
            context.drawTextWithShadow(client.textRenderer, Text.literal("Block: " + blockId),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.targetEntityHud
                && client.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult entityHit) {
            var entity = entityHit.getEntity();
            String typeId = net.minecraft.registry.Registries.ENTITY_TYPE.getId(entity.getType()).toString();
            context.drawTextWithShadow(client.textRenderer,
                    Text.literal("Entity: " + entity.getName().getString() + " (" + typeId + ")"),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }

        if (BurmaldaSettings.emptySlotsHud) {
            long empty = client.player.getInventory().main.stream().filter(net.minecraft.item.ItemStack::isEmpty).count();
            context.drawTextWithShadow(client.textRenderer, Text.literal("Empty slots: " + empty),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.selectedSlotHud) {
            context.drawTextWithShadow(client.textRenderer,
                    Text.literal("Hotbar slot: " + (client.player.getInventory().selectedSlot + 1) + "/9"),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.facingAngleHud) {
            context.drawTextWithShadow(client.textRenderer,
                    Text.literal(String.format(java.util.Locale.ROOT, "Yaw: %.1f°", client.player.getYaw())),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.verticalSpeedHud) {
            context.drawTextWithShadow(client.textRenderer,
                    Text.literal(String.format(java.util.Locale.ROOT, "Vertical speed: %.2f b/s",
                            client.player.getVelocity().y * 20.0)),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.fallDistanceHud) {
            context.drawTextWithShadow(client.textRenderer,
                    Text.literal(String.format(java.util.Locale.ROOT, "Fall distance: %.1f blocks",
                            client.player.fallDistance)),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.airHud) {
            context.drawTextWithShadow(client.textRenderer,
                    Text.literal("Air: " + client.player.getAir() + "/" + client.player.getMaxAir()),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.nearbyPlayersHud && client.world != null) {
            long count = client.world.getPlayers().stream()
                    .filter(player -> player != client.player && player.squaredDistanceTo(client.player) <= 64.0 * 64.0)
                    .count();
            context.drawTextWithShadow(client.textRenderer, Text.literal("Players nearby (64): " + count),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.nearbyMobsHud && client.world != null) {
            int count = client.world.getEntitiesByClass(net.minecraft.entity.mob.MobEntity.class,
                    client.player.getBoundingBox().expand(16.0), mob -> true).size();
            context.drawTextWithShadow(client.textRenderer, Text.literal("Mobs nearby (16): " + count),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.targetDistanceHud
                && client.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult target) {
            double distance = client.player.distanceTo(target.getEntity());
            context.drawTextWithShadow(client.textRenderer,
                    Text.literal(String.format(java.util.Locale.ROOT, "Target distance: %.1f blocks", distance)),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        } else if (BurmaldaSettings.targetDistanceHud) {
            context.drawTextWithShadow(client.textRenderer, Text.literal("Target distance: --"),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.spawnDistanceHud && client.world != null) {
            double distance = Math.sqrt(client.player.getBlockPos().getSquaredDistance(client.world.getSpawnPos()));
            context.drawTextWithShadow(client.textRenderer,
                    Text.literal(String.format(java.util.Locale.ROOT, "Spawn distance: %.1f blocks", distance)),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }


        if (BurmaldaSettings.healthHud) {
            context.drawTextWithShadow(client.textRenderer,
                    Text.literal(String.format(java.util.Locale.ROOT, "Health: %.1f / 10 hearts", client.player.getHealth() / 2.0f)),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.absorptionHud) {
            context.drawTextWithShadow(client.textRenderer,
                    Text.literal(String.format(java.util.Locale.ROOT, "Absorption: %.1f hearts", client.player.getAbsorptionAmount() / 2.0f)),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.offhandHud) {
            var offhand = client.player.getOffHandStack();
            String value = offhand.isEmpty() ? "empty" : offhand.getName().getString()
                    + (offhand.getCount() > 1 ? " x" + offhand.getCount() : "");
            context.drawTextWithShadow(client.textRenderer, Text.literal("Offhand: " + value),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.arrowCounterHud) {
            int count = client.player.getInventory().main.stream()
                    .filter(stack -> stack.isOf(net.minecraft.item.Items.ARROW)
                            || stack.isOf(net.minecraft.item.Items.SPECTRAL_ARROW)
                            || stack.isOf(net.minecraft.item.Items.TIPPED_ARROW))
                    .mapToInt(net.minecraft.item.ItemStack::getCount).sum();
            context.drawTextWithShadow(client.textRenderer, Text.literal("Arrows: " + count),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.pearlCounterHud) {
            int count = client.player.getInventory().main.stream()
                    .filter(stack -> stack.isOf(net.minecraft.item.Items.ENDER_PEARL))
                    .mapToInt(net.minecraft.item.ItemStack::getCount).sum();
            context.drawTextWithShadow(client.textRenderer, Text.literal("Ender Pearls: " + count),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.appleCounterHud) {
            int count = client.player.getInventory().main.stream()
                    .filter(stack -> stack.isOf(net.minecraft.item.Items.GOLDEN_APPLE)
                            || stack.isOf(net.minecraft.item.Items.ENCHANTED_GOLDEN_APPLE))
                    .mapToInt(net.minecraft.item.ItemStack::getCount).sum();
            context.drawTextWithShadow(client.textRenderer, Text.literal("Golden Apples: " + count),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.shieldStatusHud) {
            var main = client.player.getMainHandStack();
            var off = client.player.getOffHandStack();
            var shield = main.isOf(net.minecraft.item.Items.SHIELD) ? main
                    : off.isOf(net.minecraft.item.Items.SHIELD) ? off : net.minecraft.item.ItemStack.EMPTY;
            String value = shield.isEmpty() ? "not equipped" : shield.isDamageable()
                    ? Math.max(0, 100 - shield.getDamage() * 100 / shield.getMaxDamage()) + "%" : "ready";
            context.drawTextWithShadow(client.textRenderer, Text.literal("Shield: " + value),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.targetBlockPosHud) {
            String value = client.crosshairTarget instanceof BlockHitResult hit
                    ? hit.getBlockPos().getX() + " " + hit.getBlockPos().getY() + " " + hit.getBlockPos().getZ()
                    : "--";
            context.drawTextWithShadow(client.textRenderer, Text.literal("Target block: " + value),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.targetHealthHud) {
            String value = client.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult hit
                    && hit.getEntity() instanceof net.minecraft.entity.LivingEntity living
                    ? String.format(java.util.Locale.ROOT, "%.1f / %.1f", living.getHealth(), living.getMaxHealth())
                    : "--";
            context.drawTextWithShadow(client.textRenderer, Text.literal("Target health: " + value),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.spawnCoordsHud && client.world != null) {
            var spawn = client.world.getSpawnPos();
            context.drawTextWithShadow(client.textRenderer,
                    Text.literal("World spawn: " + spawn.getX() + " " + spawn.getY() + " " + spawn.getZ()),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }

        if (BurmaldaSettings.saturationHud) {
            context.drawTextWithShadow(client.textRenderer,
                    Text.literal(String.format(java.util.Locale.ROOT, "Saturation: %.1f", client.player.getHungerManager().getSaturationLevel())),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.groundStatusHud) {
            context.drawTextWithShadow(client.textRenderer, Text.literal("On ground: " + (client.player.isOnGround() ? "yes" : "no")),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.sprintStatusHud) {
            context.drawTextWithShadow(client.textRenderer, Text.literal("Sprinting: " + (client.player.isSprinting() ? "yes" : "no")),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.swimmingStatusHud) {
            context.drawTextWithShadow(client.textRenderer, Text.literal("Swimming: " + (client.player.isSwimming() ? "yes" : "no")),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.elytraStatusHud) {
            context.drawTextWithShadow(client.textRenderer, Text.literal("Elytra: " + (client.player.isFallFlying() ? "flying" : "inactive")),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.blockLightHud && client.world != null) {
            int light = client.world.getLightLevel(net.minecraft.world.LightType.BLOCK, client.player.getBlockPos());
            context.drawTextWithShadow(client.textRenderer, Text.literal("Block light: " + light + "/15"),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.skyLightHud && client.world != null) {
            int light = client.world.getLightLevel(net.minecraft.world.LightType.SKY, client.player.getBlockPos());
            context.drawTextWithShadow(client.textRenderer, Text.literal("Sky light: " + light + "/15"),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.moonPhaseHud && client.world != null) {
            String[] phases = {"Full moon", "Waning gibbous", "Third quarter", "Waning crescent",
                    "New moon", "Waxing crescent", "First quarter", "Waxing gibbous"};
            context.drawTextWithShadow(client.textRenderer,
                    Text.literal("Moon: " + phases[client.world.getMoonPhase() & 7]),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.biomeTemperatureHud && client.world != null) {
            float temperature = client.world.getBiome(client.player.getBlockPos()).value().getTemperature();
            context.drawTextWithShadow(client.textRenderer,
                    Text.literal(String.format(java.util.Locale.ROOT, "Biome temperature: %.2f", temperature)),
                    x, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }
        if (BurmaldaSettings.inventoryUsageHud) {
            long occupied = client.player.getInventory().main.stream().filter(stack -> !stack.isEmpty()).count();
            context.drawTextWithShadow(client.textRenderer, Text.literal("Inventory: " + occupied + "/36 slots"),
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
