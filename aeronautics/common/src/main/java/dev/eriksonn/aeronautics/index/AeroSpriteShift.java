package dev.eriksonn.aeronautics.index;

import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import net.minecraft.resources.Identifier;

/** Sprites resolve in the native model baker on every resource reload. */
public final class AeroSpriteShift {
    public static final CTSpriteShiftEntry LEVITITE = omni("block/levitite");
    public static final CTSpriteShiftEntry PEARLESCENT_LEVITITE = omni("block/pearlescent_levitite");
    private static CTSpriteShiftEntry omni(String name) {
        return new CTSpriteShiftEntry(Identifier.fromNamespaceAndPath("aeronautics", name),
                Identifier.fromNamespaceAndPath("aeronautics", name + "_connected"), CTSpriteShiftEntry.Mode.OMNIDIRECTIONAL);
    }
    private AeroSpriteShift() {}
}
