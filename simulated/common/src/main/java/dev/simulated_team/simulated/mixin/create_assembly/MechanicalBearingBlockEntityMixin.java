package dev.simulated_team.simulated.mixin.create_assembly;

import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import dev.simulated_team.simulated.mixin_interface.create_assembly.IControlContraptionExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = MechanicalBearingBlockEntity.class, remap = false)
public abstract class MechanicalBearingBlockEntityMixin implements IControlContraptionExtension  {
    @Shadow public abstract boolean disassemble();
    @Shadow protected abstract boolean isWindmill();

    @Override
    public boolean sable$disassemble() {
        return this.disassemble();
    }

    @Override
    public com.simibubi.create.content.contraptions.StructureTransform sable$restorationTransform(com.simibubi.create.content.contraptions.ControlledContraptionEntity entity) {
        return isWindmill() ? new com.simibubi.create.content.contraptions.StructureTransform(entity.blockPosition(), 0, 0, 0) : entity.getRestorationTransform();
    }
}
