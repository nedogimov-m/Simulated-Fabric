package dev.simulated_team.simulated.fabric.config;

import com.google.gson.JsonParser;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server-to-client snapshots only; this protocol exposes no remote mutation packet. */
public record AddonConfigSyncPayload(String modId, String document) implements CustomPacketPayload {
    public static final Type<AddonConfigSyncPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("simulated", "configuration_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AddonConfigSyncPayload> CODEC = CustomPacketPayload.codec(
            (packet, buffer) -> { buffer.writeUtf(packet.modId, 64); buffer.writeUtf(packet.document, 32_767); },
            buffer -> new AddonConfigSyncPayload(buffer.readUtf(64), buffer.readUtf(32_767)));

    public AddonConfigSyncPayload {
        if (modId.isBlank() || modId.length() > 64 || !JsonParser.parseString(document).isJsonObject()) {
            throw new IllegalArgumentException("Expected addon id and complete server configuration object");
        }
    }

    @Override
    public Type<AddonConfigSyncPayload> type() { return TYPE; }
}
