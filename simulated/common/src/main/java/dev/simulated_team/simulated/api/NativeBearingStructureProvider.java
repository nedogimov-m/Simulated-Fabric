package dev.simulated_team.simulated.api;

import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import net.minecraft.core.Direction;

/** Supplies addon structure rules while the real native bearing owns assembly transactions and UUIDs. */
public interface NativeBearingStructureProvider {
    BearingContraption simulated$createBearingStructure(boolean windmill, Direction facing);
}
