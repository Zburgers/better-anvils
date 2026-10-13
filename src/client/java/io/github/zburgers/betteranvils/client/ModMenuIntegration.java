package io.github.zburgers.betteranvils.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> FabricLoader.getInstance().isModLoaded("cloth-config")
                ? BetterAnvilsConfigScreen.create(parent)
                : missingClothConfig(parent);
    }

    private static Screen missingClothConfig(Screen parent) {
        return new Screen(Component.translatable("config.better-anvils.title")) {
            @Override
            protected void init() {
                addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                        .bounds(width / 2 - 100, height / 2 + 24, 200, 20).build());
            }

            @Override
            public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
                super.extractRenderState(graphics, mouseX, mouseY, delta);
                var message = Component.translatable("config.better-anvils.missing_cloth_config");
                graphics.text(font, message, (width - font.width(message)) / 2, height / 2 - 8, 0xFFFFFF);
            }

            @Override
            public void onClose() {
                minecraft.setScreenAndShow(parent);
            }
        };
    }
}
