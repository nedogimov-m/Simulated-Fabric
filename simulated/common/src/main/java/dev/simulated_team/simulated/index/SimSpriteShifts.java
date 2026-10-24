package dev.simulated_team.simulated.index;

import net.minecraft.resources.Identifier;

/** Reload-safe sprite identities; resolve the current native atlas only during extraction. */
public final class SimSpriteShifts {
    public record Shift(Identifier original, Identifier target) {}
    public static final Shift ROPE_WINCH_COIL = new Shift(
            Identifier.fromNamespaceAndPath("simulated", "block/rope_winch/winch_coil"),
            Identifier.fromNamespaceAndPath("simulated", "block/rope_winch/winch_coil_scroll"));
    public static void init() {}
    private SimSpriteShifts() {}
}
