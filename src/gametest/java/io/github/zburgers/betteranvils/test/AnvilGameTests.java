package io.github.zburgers.betteranvils.test;

import io.github.zburgers.betteranvils.AnvilCostPolicy;
import io.github.zburgers.betteranvils.BetterAnvils;
import io.github.zburgers.betteranvils.config.BetterAnvilsConfig;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;

public final class AnvilGameTests {
    @GameTest
    public void discountedEnchantmentKeepsVanillaOutput(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var sharpness = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
        var sword = new ItemStack(Items.DIAMOND_SWORD);
        var book = EnchantmentHelper.createBook(new EnchantmentInstance(sharpness, 5));
        var menu = new AnvilMenu(1, player.getInventory(), ContainerLevelAccess.NULL);
        menu.getSlot(0).set(sword);
        menu.getSlot(1).set(book);
        menu.createResult();

        int vanillaCost = Math.max(1, sharpness.value().getAnvilCost() / 2) * 5;
        int expectedCost = AnvilCostPolicy.discountedCost(vanillaCost, BetterAnvilsConfig.defaults()).orElseThrow();
        var output = menu.getSlot(2).getItem();
        helper.assertTrue(!output.isEmpty(), "Vanilla enchantment output should remain available");
        helper.assertTrue(menu.getCost() == expectedCost, "Expected discounted price " + expectedCost + " but got " + menu.getCost());
        helper.assertTrue(EnchantmentHelper.getEnchantmentsForCrafting(output).getLevel(sharpness) == 5, "Sharpness level should remain unchanged");
        helper.assertTrue(output.getDamageValue() == 0, "Durability should remain unchanged");
        helper.succeed();
    }

    @GameTest
    public void maximumNormalCostIsAllowed(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var menu = enchantedSwordMenu(helper, player, 194);
        var output = menu.getSlot(2).getItem();
        helper.assertTrue(menu.getCost() == 100 && menu.getSlot(2).hasItem(), "Cost 100 should remain selectable");
        helper.assertTrue(output.getOrDefault(DataComponents.REPAIR_COST, 0) == AnvilMenu.calculateIncreasedRepairCost(194), "Prior-work metadata should grow independently of the XP discount");
        helper.succeed();
    }

    @GameTest
    public void disablingPriorWorkTaxKeepsMetadataGrowth(GameTestHelper helper) throws Exception {
        var service = BetterAnvils.configService();
        var original = BetterAnvils.activeConfig();
        try {
            service.save(new BetterAnvilsConfig(original.maximumAnvilCost(), original.anvilDiscountPercent(), original.minimumAnvilCost(), false));
            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            var menu = enchantedSwordMenu(helper, player, 194);
            var output = menu.getSlot(2).getItem();
            helper.assertTrue(menu.getCost() == 3, "Ignoring prior-work XP tax should discount only the enchantment cost");
            helper.assertTrue(output.getOrDefault(DataComponents.REPAIR_COST, 0) == AnvilMenu.calculateIncreasedRepairCost(194), "Prior-work metadata should still grow normally");
        } finally {
            service.save(original);
        }
        helper.succeed();
    }

    @GameTest
    public void costAboveMaximumIsRejected(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var menu = enchantedSwordMenu(helper, player, 196);
        helper.assertTrue(menu.getCost() == 101 && !menu.getSlot(2).hasItem(), "Cost 101 should be rejected");
        helper.succeed();
    }

    @GameTest
    public void creativePlayerKeepsVanillaPermission(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        player.getAbilities().instabuild = true;
        var menu = enchantedSwordMenu(helper, player, 196);
        helper.assertTrue(menu.getCost() == 101 && menu.getSlot(2).hasItem(), "Creative player should keep an above-limit result");
        helper.succeed();
    }

    @GameTest
    public void renameOnlyUsesConfiguredMaximum(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(DataComponents.REPAIR_COST, 300);
        var menu = new AnvilMenu(1, player.getInventory(), ContainerLevelAccess.NULL);
        menu.getSlot(0).set(sword);
        menu.setItemName("Renamed sword");
        menu.createResult();
        helper.assertTrue(menu.getCost() == 100 && menu.getSlot(2).hasItem(), "Rename-only operation should cap at 100");
        helper.succeed();
    }

    @GameTest
    public void stackedItemPenaltyRemainsVanillaCostForty(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var sharpness = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
        var sword = new ItemStack(Items.DIAMOND_SWORD);
        var enchantedBook = EnchantmentHelper.createBook(new EnchantmentInstance(sharpness, 1));
        var menu = new AnvilMenu(1, player.getInventory(), ContainerLevelAccess.NULL);
        menu.getSlot(0).set(sword);
        menu.getSlot(0).getItem().setCount(2);
        menu.getSlot(1).set(enchantedBook);
        menu.createResult();
        helper.assertTrue(menu.getCost() == 20, "Vanilla stacked-item penalty 40 should be discounted to 20; got " + menu.getCost() + ", output=" + menu.getSlot(2).hasItem());
        helper.succeed();
    }

    private AnvilMenu enchantedSwordMenu(GameTestHelper helper, Player player, int repairCost) {
        var sharpness = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
        var sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(DataComponents.REPAIR_COST, repairCost);
        var book = EnchantmentHelper.createBook(new EnchantmentInstance(sharpness, 5));
        var menu = new AnvilMenu(1, player.getInventory(), ContainerLevelAccess.NULL);
        menu.getSlot(0).set(sword);
        menu.getSlot(1).set(book);
        menu.createResult();
        return menu;
    }
}
