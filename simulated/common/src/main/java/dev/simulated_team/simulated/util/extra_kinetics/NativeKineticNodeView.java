package dev.simulated_team.simulated.util.extra_kinetics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Typed view of the real Create graph node, including its same-cell kinetic channel. */
public interface NativeKineticNodeView {
    BlockPos simulated$position();
    Direction simulated$port();
}
