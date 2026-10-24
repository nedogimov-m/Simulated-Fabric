package dev.simulated_team.simulated.fabric.client;

import net.minecraft.world.entity.player.Input;

/** Suppress only outgoing crouch while retaining raw input for hold-release detection. */
public final class NativeHoldInput {
    public static Input filter(Input raw,boolean canCrouch) {
        return canCrouch||!raw.shift()?raw:new Input(raw.forward(),raw.backward(),raw.left(),raw.right(),raw.jump(),false,raw.sprint());
    }
    private NativeHoldInput() { }
}
