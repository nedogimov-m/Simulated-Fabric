package dev.simulated_team.simulated.mixin.create_assembly;

import com.simibubi.create.content.contraptions.bearing.ClockworkBearingBlockEntity;
import dev.simulated_team.simulated.mixin_interface.create_assembly.IControlContraptionExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ClockworkBearingBlockEntity.class, remap = false)
public abstract class ClockworkBearingBlockEntityMixin implements IControlContraptionExtension  {
    @Shadow public abstract boolean disassemble();

    @Override
    public boolean sable$disassemble() {
        return this.disassemble();
    }

    @Override
    public com.simibubi.create.content.contraptions.StructureTransform sable$restorationTransform(com.simibubi.create.content.contraptions.ControlledContraptionEntity entity) {
        // The native clock restores both hands at zero angle, independently of their displayed pose.
        return new com.simibubi.create.content.contraptions.StructureTransform(entity.blockPosition(), 0, 0, 0);
    }
}
