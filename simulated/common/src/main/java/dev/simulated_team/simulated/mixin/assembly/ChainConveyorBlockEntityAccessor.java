package dev.simulated_team.simulated.mixin.assembly;

import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Queues the native next-tick connection check when an assembler moves a conveyor. */
@Mixin(ChainConveyorBlockEntity.class)
public interface ChainConveyorBlockEntityAccessor {
    @Accessor("validate") void simulated$setConnectionValidation(boolean required);
}
