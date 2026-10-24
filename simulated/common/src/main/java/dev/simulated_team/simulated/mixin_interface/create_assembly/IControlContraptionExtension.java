package dev.simulated_team.simulated.mixin_interface.create_assembly;

import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.StructureTransform;

public interface IControlContraptionExtension {

    boolean sable$disassemble();

    default StructureTransform sable$restorationTransform(ControlledContraptionEntity entity) {
        return entity.getRestorationTransform();
    }

}
