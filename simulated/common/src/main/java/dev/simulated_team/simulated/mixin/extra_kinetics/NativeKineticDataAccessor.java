package dev.simulated_team.simulated.mixin.extra_kinetics;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(KineticBlockEntity.class)
public interface NativeKineticDataAccessor {
    @Invoker("write") void simulated$writeKinetics(ValueOutput output,boolean clientPacket);
    @Invoker("read") void simulated$readKinetics(ValueInput input,boolean clientPacket);
}
