package io.github.zburgers.betteranvils.mixin.client;

import io.github.zburgers.betteranvils.client.ClientPolicyState;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(AnvilScreen.class)
abstract class AnvilScreenMixin {
    @ModifyConstant(method = "extractLabels", constant = @Constant(intValue = 40))
    private int betterAnvils$maximumDisplayedCost(int original) {
        return ClientPolicyState.effectiveConfig().maximumAnvilCost() + 1;
    }
}
