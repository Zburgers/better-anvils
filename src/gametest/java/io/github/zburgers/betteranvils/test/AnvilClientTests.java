package io.github.zburgers.betteranvils.test;

import io.github.zburgers.betteranvils.BetterAnvils;
import io.github.zburgers.betteranvils.client.ClientPolicyState;
import io.github.zburgers.betteranvils.config.BetterAnvilsConfig;
import io.github.zburgers.betteranvils.net.AnvilPolicyPayload;
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
                    var sharpness = client.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
                    var sword = new ItemStack(Items.DIAMOND_SWORD);
                    sword.set(DataComponents.REPAIR_COST, 194);
                    var book = EnchantmentHelper.createBook(new EnchantmentInstance(sharpness, 5));
                    var menu = new AnvilMenu(1, player.getInventory(), ContainerLevelAccess.NULL);
                    menu.getSlot(0).set(sword);
                    menu.getSlot(1).set(book);
                    menu.createResult();
                    if (menu.getCost() != 100 || !menu.getSlot(2).hasItem()) {
                        throw new AssertionError("Client policy should keep the cost-100 result available");
                    }
                    client.setScreenAndShow(new AnvilScreen(menu, player.getInventory(), Component.literal("Anvil")));
                });
                context.waitForScreen(AnvilScreen.class);
                context.takeScreenshot("anvil-cost-100");
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
}
