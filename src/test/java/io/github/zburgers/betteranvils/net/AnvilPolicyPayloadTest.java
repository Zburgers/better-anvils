package io.github.zburgers.betteranvils.net;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnvilPolicyPayloadTest {
    @Test
    void codecRoundTripPreservesValidatedPolicy() {
        var payload = new AnvilPolicyPayload(AnvilPolicyPayload.SCHEMA_VERSION, 240, 75, 2, false);
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            AnvilPolicyPayload.CODEC.encode(buffer, payload);
            var decoded = AnvilPolicyPayload.CODEC.decode(buffer);
            assertEquals(payload, decoded);
            assertEquals(payload.toConfig(), decoded.toConfig());
        } finally {
            buffer.release();
        }
    }

    @Test
    void rejectsUnsupportedSchemaAndInvalidValues() {
        assertTrue(new AnvilPolicyPayload(AnvilPolicyPayload.SCHEMA_VERSION + 1, 100, 50, 1, true).toConfig().isEmpty());
        assertTrue(new AnvilPolicyPayload(AnvilPolicyPayload.SCHEMA_VERSION, 501, 50, 1, true).toConfig().isEmpty());
    }
}
