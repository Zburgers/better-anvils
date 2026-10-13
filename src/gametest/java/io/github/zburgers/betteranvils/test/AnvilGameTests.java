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
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.Enchantment;

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

    @GameTest
    public void insufficientLevelsCannotTakeResult(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.experienceLevel = 2;
        var menu = enchantedSwordMenu(helper, player, 0);
        helper.assertTrue(menu.getCost() == 3 && menu.getSlot(2).hasItem(), "Anvil should show the discounted cost");
        menu.clicked(2, 0, ContainerInput.PICKUP, player);
        helper.assertTrue(player.experienceLevel == 2 && menu.getSlot(0).hasItem() && menu.getSlot(1).hasItem(), "Insufficient XP must not charge or consume inputs");
        helper.assertTrue(menu.getSlot(2).hasItem() && menu.getCarried().isEmpty(), "Insufficient XP must leave the result untouched");
        helper.succeed();
    }

    @GameTest
    public void takingResultChargesDisplayedLevelsAndConsumesInputs(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.experienceLevel = 125;
        var menu = enchantedSwordMenu(helper, player, 194);
        var expected = menu.getCost();
        menu.clicked(2, 0, ContainerInput.PICKUP, player);
        helper.assertTrue(expected == 100 && player.experienceLevel == 25, "Pickup should subtract exactly the displayed cost");
        helper.assertTrue(menu.getCarried().getItem() == Items.DIAMOND_SWORD, "Vanilla result should move to the cursor");
        var sharpness = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
        helper.assertTrue(EnchantmentHelper.getEnchantmentsForCrafting(menu.getCarried()).getLevel(sharpness) == 5, "Pickup should preserve the Sharpness level");
        helper.assertTrue(!menu.getSlot(0).hasItem() && !menu.getSlot(1).hasItem(), "Successful pickup should consume both inputs exactly once");
        helper.succeed();
    }

    @GameTest
    public void renameKeepsRequestedName(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var menu = new AnvilMenu(1, player.getInventory(), ContainerLevelAccess.NULL);
        menu.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));
        menu.setItemName("Renamed sword");
        helper.assertTrue(menu.getSlot(2).getItem().getHoverName().getString().equals("Renamed sword"), "Rename result should keep the requested name");
        helper.succeed();
    }

    @GameTest
    public void repairMaterialKeepsVanillaDurabilityResult(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setDamageValue(100);
        var menu = new AnvilMenu(1, player.getInventory(), ContainerLevelAccess.NULL);
        menu.getSlot(0).set(sword);
        menu.getSlot(1).set(new ItemStack(Items.DIAMOND));
        menu.createResult();
        helper.assertTrue(menu.getSlot(2).hasItem() && menu.getSlot(2).getItem().getDamageValue() == 0, "Diamond repair should preserve vanilla durability calculation");
        helper.succeed();
    }

    @GameTest
    public void sacrificedItemMergeKeepsVanillaDurabilityResult(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var first = new ItemStack(Items.DIAMOND_SWORD);
        var second = new ItemStack(Items.DIAMOND_SWORD);
        first.setDamageValue(1000);
        second.setDamageValue(1000);
        var menu = new AnvilMenu(1, player.getInventory(), ContainerLevelAccess.NULL);
        menu.getSlot(0).set(first);
        menu.getSlot(1).set(second);
        menu.createResult();
        int maximum = first.getMaxDamage();
        int expectedDamage = Math.max(0, maximum - (maximum - 1000) - (maximum - 1000) - maximum * 12 / 100);
        helper.assertTrue(menu.getSlot(2).hasItem() && menu.getSlot(2).getItem().getDamageValue() == expectedDamage, "Sacrificed-item repair should keep vanilla durability formula");
        helper.succeed();
    }

    @GameTest
    public void anvilHonorsDataDrivenEnchantmentCompatibility(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var enchantments = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var sharpness = enchantments.getOrThrow(Enchantments.SHARPNESS);
        var smite = enchantments.getOrThrow(Enchantments.SMITE);
        boolean compatible = Enchantment.areCompatible(sharpness, smite);
        var sword = new ItemStack(Items.DIAMOND_SWORD);
        EnchantmentHelper.updateEnchantments(sword, mutable -> mutable.set(sharpness, 5));
        var menu = new AnvilMenu(1, player.getInventory(), ContainerLevelAccess.NULL);
        menu.getSlot(0).set(sword);
        menu.getSlot(1).set(EnchantmentHelper.createBook(new EnchantmentInstance(smite, 5)));
        menu.createResult();
        if (compatible) {
            var output = menu.getSlot(2).getItem();
            helper.assertTrue(!output.isEmpty(), "Data-compatible enchantments should combine");
            var resultEnchantments = EnchantmentHelper.getEnchantmentsForCrafting(output);
            helper.assertTrue(resultEnchantments.getLevel(sharpness) == 5 && resultEnchantments.getLevel(smite) == 5,
                    "Both data-compatible enchantments should remain on the result");
        } else {
            helper.assertTrue(!menu.getSlot(2).hasItem() && menu.getCost() == 0,
                    "Data-incompatible enchantment-only operation should remain invalid");
        }
        helper.succeed();
    }

    @GameTest
    public void datapackEnchantmentLevelsArePreserved(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var sharpness = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
        int maximum = sharpness.value().getMaxLevel();
        if (Boolean.getBoolean("betterAnvils.requireHigherEnchants")) {
            helper.assertTrue(maximum == 8, "HigherEnchants 1.4 should set Sharpness maximum to 8; got " + maximum);
        }
        if (maximum <= 5) {
            helper.succeed();
            return;
        }

        var menu = new AnvilMenu(1, player.getInventory(), ContainerLevelAccess.NULL);
        menu.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));
        menu.getSlot(1).set(EnchantmentHelper.createBook(new EnchantmentInstance(sharpness, maximum)));
        menu.createResult();
        var output = menu.getSlot(2).getItem();
        helper.assertTrue(!output.isEmpty(), "Datapack-supported high-level enchantment should be applicable");
        helper.assertTrue(EnchantmentHelper.getEnchantmentsForCrafting(output).getLevel(sharpness) == maximum,
                "Anvil output should preserve the datapack-defined enchantment level");
        helper.succeed();
    }

    @GameTest
    public void emptyAnvilDoesNotForgeResult(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var menu = new AnvilMenu(1, player.getInventory(), ContainerLevelAccess.NULL);
        menu.createResult();
        helper.assertTrue(!menu.getSlot(2).hasItem() && menu.getCost() == 0, "Empty inputs should keep the result and cost empty");
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
