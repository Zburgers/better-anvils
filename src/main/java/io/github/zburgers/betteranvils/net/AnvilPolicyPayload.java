package io.github.zburgers.betteranvils.net;

import io.github.zburgers.betteranvils.BetterAnvils;
import io.github.zburgers.betteranvils.config.BetterAnvilsConfig;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public record AnvilPolicyPayload(int schemaVersion, int maximumAnvilCost, int anvilDiscountPercent, int minimumAnvilCost,
                                 boolean preservePriorWorkPenalty) implements CustomPacketPayload {
    public static final int SCHEMA_VERSION = 1;
    public static final Type<AnvilPolicyPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(BetterAnvils.MOD_ID, "anvil_policy"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AnvilPolicyPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.schemaVersion);
                buffer.writeVarInt(payload.maximumAnvilCost);
                buffer.writeVarInt(payload.anvilDiscountPercent);
                buffer.writeVarInt(payload.minimumAnvilCost);
                buffer.writeBoolean(payload.preservePriorWorkPenalty);
            },
            buffer -> new AnvilPolicyPayload(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean())
    );

    public static AnvilPolicyPayload from(BetterAnvilsConfig config) {
        var valid = config.requireValid();
        return new AnvilPolicyPayload(SCHEMA_VERSION, valid.maximumAnvilCost(), valid.anvilDiscountPercent(), valid.minimumAnvilCost(), valid.preservePriorWorkPenalty());
    }

    public Optional<BetterAnvilsConfig> toConfig() {
        if (schemaVersion != SCHEMA_VERSION) {
            return Optional.empty();
        }
        var config = new BetterAnvilsConfig(maximumAnvilCost, anvilDiscountPercent, minimumAnvilCost, preservePriorWorkPenalty);
        return config.validationErrors().isEmpty() ? Optional.of(config.snapshot()) : Optional.empty();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
