package io.github.zburgers.betteranvils.test;

import io.github.zburgers.betteranvils.AnvilCostPolicy;
import io.github.zburgers.betteranvils.config.BetterAnvilsConfig;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
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
}
