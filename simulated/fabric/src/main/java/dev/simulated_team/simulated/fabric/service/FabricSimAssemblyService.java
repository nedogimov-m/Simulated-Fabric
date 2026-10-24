package dev.simulated_team.simulated.fabric.service;

import dev.simulated_team.simulated.mixin.assembly.NativePistonAdhesionAccessor;
import dev.simulated_team.simulated.service.SimAssemblyService;
import net.minecraft.world.level.block.state.BlockState;

public final class FabricSimAssemblyService implements SimAssemblyService {
    @Override public boolean canStickTo(BlockState first, BlockState second) {
        return NativePistonAdhesionAccessor.simulated$canStick(first, second);
    }
}
