package io.github.zburgers.betteranvils.test;

import io.github.zburgers.betteranvils.BetterAnvils;
import io.github.zburgers.betteranvils.client.BetterAnvilsConfigScreen;
import io.github.zburgers.betteranvils.client.ClientPolicyState;
import io.github.zburgers.betteranvils.config.BetterAnvilsConfig;
import io.github.zburgers.betteranvils.net.AnvilPolicyPayload;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.gui.ClothConfigScreen;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.gui.entries.IntegerListEntry;
import me.shedaniel.clothconfig2.gui.entries.IntegerSliderEntry;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;

import java.nio.file.Files;
import java.util.List;

public final class AnvilClientTests implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        var original = BetterAnvils.activeConfig();
        try {
            BetterAnvils.saveConfig(new BetterAnvilsConfig(150, 50, 1, true));
            try (var singleplayer = context.worldBuilder().setUseConsistentSettings(true).create()) {
                context.waitFor(client -> client.player != null);
                context.waitFor(client -> ClientPolicyState.hasServerConfig()
                        && ClientPolicyState.effectiveConfig().maximumAnvilCost() == 150);
                context.runOnClient(client -> {
                    var player = client.player;
                    player.experienceLevel = 125;
                    var menu = sharpnessMenu(client, player);
                    if (menu.getCost() != 100 || !menu.getSlot(2).hasItem()) {
                        throw new AssertionError("Client policy should keep the cost-100 result available");
                    }
                    client.setScreenAndShow(new AnvilScreen(menu, player.getInventory(), Component.literal("Anvil")));
                });
                context.waitForScreen(AnvilScreen.class);
                context.takeScreenshot("anvil-cost-100");
                exerciseSettingsScreen(context);
            }
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        } finally {
            ClientPolicyState.clear();
            try {
                BetterAnvils.saveConfig(original);
            } catch (java.io.IOException exception) {
                throw new RuntimeException(exception);
            }
        }
    }

    private void exerciseSettingsScreen(ClientGameTestContext context) throws Exception {
        var current = BetterAnvils.activeConfig();
        var configFile = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("better-anvils.json");
        var beforeCancel = Files.readString(configFile);
        var cancelScreen = context.computeOnClient(client -> {
            var screen = (ClothConfigScreen) BetterAnvilsConfigScreen.create(client.gui.screen());
            var discount = entry(screen, "Anvil discount", IntegerSliderEntry.class);
            discount.setValue(75);
            client.setScreenAndShow(screen);
            if (BetterAnvils.activeConfig().anvilDiscountPercent() != current.anvilDiscountPercent()) {
                throw new AssertionError("Changing a draft must not update active settings");
            }
            return screen;
        });
        context.waitForScreen(ClothConfigScreen.class);
        context.takeScreenshot("better-anvils-config");
        if (!context.tryClickScreenButton("Discard Changes")) {
            context.runOnClient(client -> cancelScreen.onClose());
        }
        context.runOnClient(client -> {
            if (!BetterAnvils.activeConfig().equals(current)) {
                throw new AssertionError("Cancel must leave active settings unchanged");
            }
        });
        if (!Files.readString(configFile).equals(beforeCancel)) {
            throw new AssertionError("Cancel must leave the saved config file unchanged");
        }

        context.runOnClient(client -> {
            var screen = (ClothConfigScreen) BetterAnvilsConfigScreen.create(client.gui.screen());
            entry(screen, "Anvil discount", IntegerSliderEntry.class).setValue(75);
            client.setScreenAndShow(screen);
        });
        context.waitForScreen(ClothConfigScreen.class);
        if (!context.tryClickScreenButton("Save & Quit")) {
            throw new AssertionError("Settings screen should expose a Save button");
        }
        context.waitFor(client -> BetterAnvils.activeConfig().anvilDiscountPercent() == 75);
        context.waitFor(client -> ClientPolicyState.effectiveConfig().anvilDiscountPercent() == 75);
        context.runOnClient(client -> {
            var menu = sharpnessMenu(client, client.player);
            if (menu.getCost() != 50 || !menu.getSlot(2).hasItem()) {
                throw new AssertionError("Saving 75% should update the integrated-server anvil price");
            }
        });
        var savedJson = Files.readString(configFile);
        if (!savedJson.contains("\"anvilDiscountPercent\": 75")) {
            throw new AssertionError("Save should write the selected discount to the config file");
        }

        context.runOnClient(client -> {
            var screen = (ClothConfigScreen) BetterAnvilsConfigScreen.create(client.gui.screen());
            var maximum = entry(screen, "Maximum anvil cost", IntegerListEntry.class);
            var discount = entry(screen, "Anvil discount", IntegerSliderEntry.class);
            var minimum = entry(screen, "Minimum nonzero cost", IntegerListEntry.class);
            maximum.setValue(maximum.getDefaultValue().orElseThrow().toString());
            discount.setValue(discount.getDefaultValue().orElseThrow());
            minimum.setValue(minimum.getDefaultValue().orElseThrow().toString());
            if (!BetterAnvils.activeConfig().equals(new BetterAnvilsConfig(150, 75, 1, true))) {
                throw new AssertionError("Resetting the draft must wait for Save");
            }
            screen.saveAll(false);
        });
        context.waitFor(client -> BetterAnvils.activeConfig().equals(BetterAnvilsConfig.defaults()));
        context.waitFor(client -> ClientPolicyState.effectiveConfig().equals(BetterAnvilsConfig.defaults()));
        context.runOnClient(client -> {
            var menu = sharpnessMenu(client, client.player);
            if (menu.getCost() != 100 || !menu.getSlot(2).hasItem()) {
                throw new AssertionError("Resetting defaults should restore the 100-level anvil result");
            }
        });
        var resetJson = Files.readString(configFile);
        if (!resetJson.contains("\"maximumAnvilCost\": 100")
                || !resetJson.contains("\"anvilDiscountPercent\": 50")
                || !resetJson.contains("\"minimumAnvilCost\": 1")
                || !resetJson.contains("\"preservePriorWorkPenalty\": true")) {
            throw new AssertionError("Saving reset defaults should persist the full settings snapshot");
        }

        var defaultScreen = context.computeOnClient(client -> {
            var screen = (ClothConfigScreen) BetterAnvilsConfigScreen.create(client.gui.screen());
            if (entry(screen, "Maximum anvil cost", IntegerListEntry.class).getValue() != 100
                    || entry(screen, "Anvil discount", IntegerSliderEntry.class).getValue() != 50
                    || entry(screen, "Minimum nonzero cost", IntegerListEntry.class).getValue() != 1
                    || !entry(screen, "Preserve prior work XP penalty", BooleanListEntry.class).getValue()) {
                throw new AssertionError("Settings screen should present the documented defaults");
            }
            client.setScreenAndShow(screen);
            return screen;
        });
        context.waitForScreen(ClothConfigScreen.class);
        context.takeScreenshot("better-anvils-config-default");
        if (!context.tryClickScreenButton("Discard Changes")) {
            context.runOnClient(client -> defaultScreen.onClose());
        }

        var beforeInvalid = Files.readString(configFile);
        context.runOnClient(client -> {
            var screen = (ClothConfigScreen) BetterAnvilsConfigScreen.create(client.gui.screen());
            entry(screen, "Maximum anvil cost", IntegerListEntry.class).setValue("1");
            var minimum = entry(screen, "Minimum nonzero cost", IntegerListEntry.class);
            minimum.setValue("5");
            if (minimum.getConfigError().isEmpty()) {
                throw new AssertionError("Minimum above maximum should be invalid");
            }
            client.setScreenAndShow(screen);
        });
        context.waitForScreen(ClothConfigScreen.class);
        context.takeScreenshot("better-anvils-config-invalid");
        if (context.tryClickScreenButton("Save & Quit")) {
            throw new AssertionError("Invalid settings must disable Save & Quit");
        }
        context.runOnClient(client -> {
            if (!BetterAnvils.activeConfig().equals(BetterAnvilsConfig.defaults())) {
                throw new AssertionError("Invalid settings must not be saved");
            }
            client.gui.screen().onClose();
        });
        if (!Files.readString(configFile).equals(beforeInvalid)) {
            throw new AssertionError("Invalid settings must leave the saved config file unchanged");
        }
    }

    private static <T extends AbstractConfigListEntry<?>> T entry(ClothConfigScreen screen, String label, Class<T> type) {
        return screen.getCategorizedEntries().values().stream().flatMap(List::stream)
                .filter(value -> value.getFieldName().getString().equals(label))
                .map(type::cast).findFirst().orElseThrow();
    }

    private static AnvilMenu sharpnessMenu(net.minecraft.client.Minecraft client, net.minecraft.world.entity.player.Player player) {
        var sharpness = client.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
        var sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(DataComponents.REPAIR_COST, 194);
        var book = EnchantmentHelper.createBook(new EnchantmentInstance(sharpness, 5));
        var menu = new AnvilMenu(1, player.getInventory(), ContainerLevelAccess.NULL);
        menu.getSlot(0).set(sword);
        menu.getSlot(1).set(book);
        menu.createResult();
        return menu;
    }
}
