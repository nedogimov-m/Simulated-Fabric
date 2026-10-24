package dev.simulated_team.simulated.mixin.assembly;

import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Invoke the actual vanilla adhesion compatibility rule; never duplicate its block logic. */
@Mixin(PistonStructureResolver.class)
public interface NativePistonAdhesionAccessor {
    @Invoker("canStickToEachOther")
    static boolean simulated$canStick(BlockState first, BlockState second) { throw new AssertionError(); }
}
