package io.github.zburgers.betteranvils;

import io.github.zburgers.betteranvils.config.BetterAnvilsConfig;
import io.github.zburgers.betteranvils.config.ConfigService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BetterAnvils implements ModInitializer {
    public static final String MOD_ID = "better-anvils";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static volatile ConfigService config;

    @Override
    public void onInitialize() {
        config = new ConfigService(FabricLoader.getInstance().getConfigDir().resolve("better-anvils.json"), LOGGER::warn);
        config.load();
        LOGGER.info("Better Anvils initialized");
    }

    public static BetterAnvilsConfig activeConfig() {
        var current = config;
        return current == null ? BetterAnvilsConfig.defaults() : current.current();
    }

    public static ConfigService configService() {
        return config;
    }
}
