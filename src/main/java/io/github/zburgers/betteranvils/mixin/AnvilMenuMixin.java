package io.github.zburgers.betteranvils.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.zburgers.betteranvils.AnvilCostPolicy;
import io.github.zburgers.betteranvils.BetterAnvils;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(AnvilMenu.class)
abstract class AnvilMenuMixin {
    @ModifyExpressionValue(
            method = "createResult",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(JJJ)J")
    )
    private long betterAnvils$discountValidCost(long original) {
        if (original <= 0) {
            return original;
        }
        return AnvilCostPolicy.discountedCost(Math.toIntExact(original), BetterAnvils.effectiveConfig()).orElseThrow();
    }

    @ModifyArg(
            method = "createResult",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(JJJ)J"),
            index = 0
    )
    private long betterAnvils$excludePriorWorkTax(long original, @Local(index = 3) long priorWorkTax) {
        return BetterAnvils.effectiveConfig().preservePriorWorkPenalty() ? original : original - priorWorkTax;
    }

    @ModifyConstant(method = "createResult", constant = @Constant(intValue = 40, ordinal = 1))
    private int betterAnvils$renameThreshold(int original) {
        return BetterAnvils.effectiveConfig().maximumAnvilCost() + 1;
    }

    @ModifyConstant(method = "createResult", constant = @Constant(intValue = 39))
    private int betterAnvils$renameMaximum(int original) {
        return BetterAnvils.effectiveConfig().maximumAnvilCost();
    }

    @ModifyConstant(method = "createResult", constant = @Constant(intValue = 40, ordinal = 2))
    private int betterAnvils$survivalThreshold(int original) {
        return BetterAnvils.effectiveConfig().maximumAnvilCost() + 1;
    }
}
