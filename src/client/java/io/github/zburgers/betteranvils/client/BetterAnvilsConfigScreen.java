package io.github.zburgers.betteranvils.client;

import io.github.zburgers.betteranvils.BetterAnvils;
import io.github.zburgers.betteranvils.config.BetterAnvilsConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.gui.ClothConfigScreen;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.gui.entries.IntegerListEntry;
import me.shedaniel.clothconfig2.gui.entries.IntegerSliderEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class BetterAnvilsConfigScreen {
    private BetterAnvilsConfigScreen() {}

    public static Screen create(Screen parent) {
        var client = Minecraft.getInstance();
        if (client.getConnection() != null && client.getSingleplayerServer() == null) {
            return readOnly(parent, ClientPolicyState.effectiveConfig());
        }
        return editable(parent, BetterAnvils.activeConfig());
    }

    private static Screen readOnly(Screen parent, BetterAnvilsConfig config) {
        var builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.better-anvils.title"))
                .setEditable(false);
        var category = builder.getOrCreateCategory(Component.translatable("config.better-anvils.category"));
        var entries = builder.entryBuilder();
        category.addEntry(entries.startTextDescription(Component.translatable("config.better-anvils.server_controlled")).build());
        category.addEntry(entries.startTextDescription(Component.translatable("config.better-anvils.maximum.value", config.maximumAnvilCost())).build());
        category.addEntry(entries.startTextDescription(Component.translatable("config.better-anvils.discount.value", config.anvilDiscountPercent())).build());
        category.addEntry(entries.startTextDescription(Component.translatable("config.better-anvils.minimum.value", config.minimumAnvilCost())).build());
        category.addEntry(entries.startTextDescription(Component.translatable("config.better-anvils.prior_work_value", config.preservePriorWorkPenalty())).build());
        return builder.build();
    }

    private static Screen editable(Screen parent, BetterAnvilsConfig config) {
        var builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.better-anvils.title"));
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.better-anvils.category"));
        var fields = builder.entryBuilder();
        var minimumReference = new AtomicReference<IntegerListEntry>();
        var maximum = fields.startIntField(Component.translatable("config.better-anvils.maximum"), config.maximumAnvilCost())
                .setMin(1).setMax(500).setDefaultValue(100)
                .setErrorSupplier(value -> {
                    var minimum = minimumReference.get();
                    return minimum != null && value < minimum.getValue()
                            ? Optional.of(Component.translatable("config.better-anvils.minimum_exceeds_maximum"))
                            : Optional.empty();
                }).build();
        var discount = fields.startIntSlider(Component.translatable("config.better-anvils.discount"), config.anvilDiscountPercent(), 0, 100)
                .setDefaultValue(50)
                .setTextGetter(value -> Component.literal(value + "%"))
                .build();
        var minimum = fields.startIntField(Component.translatable("config.better-anvils.minimum"), config.minimumAnvilCost())
                .setMin(1).setMax(10).setDefaultValue(1)
                .setErrorSupplier(value -> value > maximum.getValue()
                        ? Optional.of(Component.translatable("config.better-anvils.minimum_exceeds_maximum"))
                        : Optional.empty()).build();
        minimumReference.set(minimum);
        var priorWork = fields.startBooleanToggle(Component.translatable("config.better-anvils.prior_work"), config.preservePriorWorkPenalty())
                .setDefaultValue(true)
                .build();

        category.addEntry(maximum);
        category.addEntry(discount);
        category.addEntry(minimum);
        category.addEntry(priorWork);
        category.addEntry(fields.startTextDescription(Component.translatable("config.better-anvils.reopen_anvil")).build());
        builder.setSavingRunnable(() -> save(maximum, discount, minimum, priorWork));
        return builder.build();
    }

    private static void save(IntegerListEntry maximum, IntegerSliderEntry discount, IntegerListEntry minimum, BooleanListEntry priorWork) {
        var config = new BetterAnvilsConfig(maximum.getValue(), discount.getValue(), minimum.getValue(), priorWork.getValue());
        try {
            BetterAnvils.saveConfig(config);
        } catch (IOException | IllegalArgumentException exception) {
            BetterAnvils.LOGGER.error("Could not save Better Anvils configuration", exception);
        }
    }
}
