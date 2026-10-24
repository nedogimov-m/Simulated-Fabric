package dev.simulated_team.simulated.mixin.create_assembly;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import dev.simulated_team.simulated.api.NativeBearingStructureProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import java.util.Objects;

/** Changes only the structure factory; native reservation/commit/recovery/disassembly remain intact. */
@Mixin(value=MechanicalBearingBlockEntity.class,remap=false)
public abstract class NativeBearingStructureFactoryMixin {
    @Shadow protected abstract boolean isWindmill();
    @ModifyExpressionValue(method="assemble",at=@At(value="NEW",target="(ZLnet/minecraft/core/Direction;)Lcom/simibubi/create/content/contraptions/bearing/BearingContraption;"),require=1,expect=1)
    private BearingContraption simulated$addonStructure(BearingContraption original) {
        return this instanceof NativeBearingStructureProvider provider
                ? Objects.requireNonNull(provider.simulated$createBearingStructure(isWindmill(),original.getFacing()),"Addon bearing structure")
                : original;
    }
}
