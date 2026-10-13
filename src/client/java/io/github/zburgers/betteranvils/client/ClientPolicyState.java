package io.github.zburgers.betteranvils.client;

import io.github.zburgers.betteranvils.BetterAnvils;
import io.github.zburgers.betteranvils.config.BetterAnvilsConfig;
import io.github.zburgers.betteranvils.net.AnvilPolicyPayload;

public final class ClientPolicyState {
    private static volatile BetterAnvilsConfig serverConfig;

    private ClientPolicyState() {}

    public static boolean accept(AnvilPolicyPayload payload) {
        var decoded = payload.toConfig();
        if (decoded.isEmpty()) {
            serverConfig = null;
            return false;
        }
        serverConfig = decoded.get();
        return true;
    }

    public static BetterAnvilsConfig effectiveConfig() {
        var current = serverConfig;
        return current == null ? BetterAnvils.activeConfig() : current;
    }

    public static boolean hasServerConfig() {
        return serverConfig != null;
    }

    public static void clear() {
        serverConfig = null;
    }
}
