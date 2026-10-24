package dev.simulated_team.simulated.network.packets;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerLevelRopeManager;
import foundry.veil.api.network.handler.ServerPacketContext;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public record RopeRidingPacket(UUID uuid, boolean stop) implements CustomPacketPayload {
    public static Type<RopeRidingPacket> TYPE = new Type<>(Simulated.path("ride_rope"));

    public static StreamCodec<RegistryFriendlyByteBuf, RopeRidingPacket> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, RopeRidingPacket::uuid,
            ByteBufCodecs.BOOL, RopeRidingPacket::stop,
            RopeRidingPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(final ServerPacketContext ctx) {
        final ServerPlayer player = ctx.player();

        final var manager = ServerLevelRopeManager.getOrCreate(player.level());
        if (manager != null) manager.ridingHeartbeat(player, this.uuid, this.stop);
    }
}
