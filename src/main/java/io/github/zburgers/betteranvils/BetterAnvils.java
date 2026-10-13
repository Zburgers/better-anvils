package io.github.zburgers.betteranvils;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BetterAnvils implements ModInitializer {
    public static final String MOD_ID = "better-anvils";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Better Anvils initialized");
    }
}
