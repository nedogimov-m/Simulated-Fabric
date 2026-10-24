package dev.simulated_team.simulated.mixin.accessor;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Native source-renewal calculation, including fluid rules and support below. */
@Mixin(FlowingFluid.class)
public interface NativeFlowingFluidAccessor {
    @Invoker("getNewLiquid")
    FluidState simulated$getNewLiquid(ServerLevel level, BlockPos pos, BlockState state);
}
