package dev.simulated_team.simulated.mixin.accessor;

import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exact native scalar state; rotor ownership stays inside MechanicalBearingBlockEntity. */
@Mixin(value=MechanicalBearingBlockEntity.class,remap=false)
public interface NativeMechanicalBearingAccessor {
    @Accessor("angle") float simulated$getNativeAngle();
    @Accessor("clientAngleDiff") float simulated$getClientAngleDiff();
    @Accessor("assembleNextTick") void simulated$setAssemblyRequested(boolean requested);
}
