package io.github.zburgers.betteranvils.client;

import io.github.zburgers.betteranvils.BetterAnvils;
import io.github.zburgers.betteranvils.config.BetterAnvilsConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.gui.entries.IntegerListEntry;
import me.shedaniel.clothconfig2.gui.entries.IntegerSliderEntry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class BetterAnvilsConfigScreen {
    private static volatile PendingSaveFailure pendingSaveFailure;

    private record PendingSaveFailure(Screen editor, Screen parent, BetterAnvilsConfig draft) {}

    static {
        // Cloth Config calls its save callback before navigating back to the parent screen.
        // Display the failure on the following tick so it is not overwritten by that navigation.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var failed = pendingSaveFailure;
            if (failed != null) {
                pendingSaveFailure = null;
                client.setScreenAndShow(new SaveErrorScreen(failed.editor(), failed.parent(), failed.draft()));
            }
        });
    }

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
        var editor = new AtomicReference<Screen>();
        builder.setSavingRunnable(() -> save(maximum, discount, minimum, priorWork, editor.get(), parent));
        var screen = builder.build();
        editor.set(screen);
        return screen;
    }

    private static void save(IntegerListEntry maximum, IntegerSliderEntry discount, IntegerListEntry minimum,
                             BooleanListEntry priorWork, Screen editor, Screen parent) {
        var draft = new BetterAnvilsConfig(maximum.getValue(), discount.getValue(), minimum.getValue(), priorWork.getValue());
        try {
            BetterAnvils.saveConfig(draft);
        } catch (IOException | IllegalArgumentException exception) {
            BetterAnvils.LOGGER.error("Could not save Better Anvils configuration", exception);
            pendingSaveFailure = new PendingSaveFailure(editor, parent, draft);
        }
    }

    /**
     * Save failures must not look like successful saves. Keep the exact failed draft
     * for Retry, and return to the original editor for changes or cancellation.
     */
    public static final class SaveErrorScreen extends Screen {
        private final Screen editor;
        private final Screen parent;
        private final BetterAnvilsConfig draft;

        private SaveErrorScreen(Screen editor, Screen parent, BetterAnvilsConfig draft) {
            super(Component.translatable("config.better-anvils.title"));
            this.editor = editor;
            this.parent = parent;
            this.draft = draft;
        }

        @Override
        protected void init() {
            addRenderableWidget(Button.builder(Component.translatable("config.better-anvils.retry_save"),
                            button -> retrySave())
                    .bounds(width / 2 - 100, height / 2 + 4, 200, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("config.better-anvils.back_to_settings"),
                            button -> minecraft.setScreenAndShow(editor))
                    .bounds(width / 2 - 100, height / 2 + 28, 200, 20).build());
        }

        private void retrySave() {
            try {
                BetterAnvils.saveConfig(draft);
                minecraft.setScreenAndShow(parent);
            } catch (IOException | IllegalArgumentException exception) {
                BetterAnvils.LOGGER.error("Could not retry Better Anvils configuration save", exception);
                // The failed draft and this error screen stay open for another retry.
            }
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            super.extractRenderState(graphics, mouseX, mouseY, delta);
            var warning = Component.translatable("config.better-anvils.save_failed");
            graphics.text(font, warning, (width - font.width(warning)) / 2, height / 2 - 32, 0xFF5555);
        }

        @Override
        public void onClose() {
            minecraft.setScreenAndShow(editor);
        }
    }
}
