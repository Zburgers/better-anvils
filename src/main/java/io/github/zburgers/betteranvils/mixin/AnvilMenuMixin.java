package io.github.zburgers.betteranvils.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.zburgers.betteranvils.AnvilCostPolicy;
import io.github.zburgers.betteranvils.BetterAnvils;
import io.github.zburgers.betteranvils.config.BetterAnvilsConfig;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
abstract class AnvilMenuMixin {
    @Unique
    private boolean betterAnvils$clientSide;

    @Inject(
            method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
            at = @At("TAIL")
    )
    private void betterAnvils$captureMenuSide(int containerId, Inventory inventory, ContainerLevelAccess access, CallbackInfo ci) {
        betterAnvils$clientSide = inventory.player.level().isClientSide();
    }

    @Unique
    private BetterAnvilsConfig betterAnvils$config() {
        return BetterAnvils.effectiveConfig(betterAnvils$clientSide);
    }
    @ModifyExpressionValue(
            method = "createResult",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(JJJ)J")
    )
    private long betterAnvils$discountValidCost(long original) {
        if (original <= 0) {
            return original;
        }
        return AnvilCostPolicy.discountedCost(Math.toIntExact(original), betterAnvils$config()).orElseThrow();
    }

    @ModifyArg(
            method = "createResult",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(JJJ)J"),
            index = 0
    )
    private long betterAnvils$excludePriorWorkTax(long original, @Local(index = 3) long priorWorkTax) {
        return betterAnvils$config().preservePriorWorkPenalty() ? original : original - priorWorkTax;
    }

    @ModifyConstant(method = "createResult", constant = @Constant(intValue = 40, ordinal = 1))
    private int betterAnvils$renameThreshold(int original) {
        return betterAnvils$config().maximumAnvilCost() + 1;
    }

    @ModifyConstant(method = "createResult", constant = @Constant(intValue = 39))
    private int betterAnvils$renameMaximum(int original) {
        return betterAnvils$config().maximumAnvilCost();
    }

    @ModifyConstant(method = "createResult", constant = @Constant(intValue = 40, ordinal = 2))
    private int betterAnvils$survivalThreshold(int original) {
        return betterAnvils$config().maximumAnvilCost() + 1;
    }
}
