package dev.simulated_team.simulated.content.physics_staff;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * An action the player can complete using the physics staff
 */
public enum PhysicsStaffAction {
    STOP_DRAG,
    LOCK,
    START_DRAG;

    public static final StreamCodec<ByteBuf, PhysicsStaffAction> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(id -> {
        if (id < 0 || id >= values().length) throw new io.netty.handler.codec.DecoderException("Unknown physics staff action: " + id);
        return values()[id];
    }, PhysicsStaffAction::ordinal);
}
