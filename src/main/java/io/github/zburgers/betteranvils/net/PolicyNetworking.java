package io.github.zburgers.betteranvils.net;

import io.github.zburgers.betteranvils.BetterAnvils;
import io.github.zburgers.betteranvils.config.BetterAnvilsConfig;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class PolicyNetworking {
    private static volatile MinecraftServer server;

    private PolicyNetworking() {}

    public static void initialize() {
        PayloadTypeRegistry.clientboundPlay().register(AnvilPolicyPayload.TYPE, AnvilPolicyPayload.CODEC);
        ServerLifecycleEvents.SERVER_STARTED.register(started -> server = started);
        ServerLifecycleEvents.SERVER_STOPPED.register(stopped -> {
            if (server == stopped) {
                server = null;
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, gameServer) -> send(handler.player, BetterAnvils.activeConfig()));
    }

    public static void broadcast(BetterAnvilsConfig config) {
        var activeServer = server;
        if (activeServer != null) {
            activeServer.execute(() -> activeServer.getPlayerList().getPlayers().forEach(player -> send(player, config)));
        }
    }

    private static void send(ServerPlayer player, BetterAnvilsConfig config) {
        if (!ServerPlayNetworking.canSend(player, AnvilPolicyPayload.TYPE)) {
            player.connection.disconnect(Component.literal("Better Anvils requires matching client and server versions."));
            return;
        }
        ServerPlayNetworking.send(player, AnvilPolicyPayload.from(config));
    }
}
