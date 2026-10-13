package io.github.zburgers.betteranvils;

import io.github.zburgers.betteranvils.config.BetterAnvilsConfig;
import io.github.zburgers.betteranvils.config.ConfigService;
import io.github.zburgers.betteranvils.net.PolicyNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public final class BetterAnvils implements ModInitializer {
    public static final String MOD_ID = "better-anvils";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static volatile ConfigService config;
    private static volatile Supplier<BetterAnvilsConfig> effectiveConfig = BetterAnvils::activeConfig;

    @Override
    public void onInitialize() {
        config = new ConfigService(FabricLoader.getInstance().getConfigDir().resolve("better-anvils.json"), LOGGER::warn);
        config.load();
        PolicyNetworking.initialize();
        LOGGER.info("Better Anvils initialized");
    }

    public static BetterAnvilsConfig activeConfig() {
        var current = config;
        return current == null ? BetterAnvilsConfig.defaults() : current.current();
    }

    public static BetterAnvilsConfig effectiveConfig() {
        return effectiveConfig.get().snapshot();
    }

    public static void useClientPolicy(Supplier<BetterAnvilsConfig> supplier) {
        effectiveConfig = supplier;
    }

    public static ConfigService configService() {
        return config;
    }

    public static void saveConfig(BetterAnvilsConfig draft) throws java.io.IOException {
        var current = config;
        if (current == null) {
            throw new IllegalStateException("Better Anvils has not initialized");
        }
        current.save(draft);
        PolicyNetworking.broadcast(current.current());
    }
}
