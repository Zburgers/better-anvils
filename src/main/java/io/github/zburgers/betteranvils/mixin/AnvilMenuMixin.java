package io.github.zburgers.betteranvils.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.zburgers.betteranvils.AnvilCostPolicy;
import io.github.zburgers.betteranvils.BetterAnvils;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

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
        return AnvilCostPolicy.discountedCost(Math.toIntExact(original), BetterAnvils.activeConfig()).orElseThrow();
    }
}
