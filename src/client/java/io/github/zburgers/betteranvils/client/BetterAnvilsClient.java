package io.github.zburgers.betteranvils.client;

import io.github.zburgers.betteranvils.BetterAnvils;
import io.github.zburgers.betteranvils.net.AnvilPolicyPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.chat.Component;

public final class BetterAnvilsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BetterAnvils.useClientPolicy(ClientPolicyState::effectiveConfig);
        ClientPlayNetworking.registerGlobalReceiver(AnvilPolicyPayload.TYPE, (payload, context) -> context.client().execute(() -> {
            if (!ClientPolicyState.accept(payload)) {
                BetterAnvils.LOGGER.warn("Rejected unsupported or invalid server anvil policy payload");
                context.client().disconnectFromWorld(Component.literal("The server sent an unsupported Better Anvils policy."));
            }
        }));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientPolicyState.clear());
    }
}
