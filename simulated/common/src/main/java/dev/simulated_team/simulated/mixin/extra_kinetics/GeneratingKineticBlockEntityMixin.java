package dev.simulated_team.simulated.mixin.extra_kinetics;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import dev.simulated_team.simulated.util.extra_kinetics.NativeKineticChannels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native loaded-owner resolution is shared; only secondary source IDs need their persistent channel. */
@Mixin(value=GeneratingKineticBlockEntity.class,remap=false)
public abstract class GeneratingKineticBlockEntityMixin {
    @Inject(method="createNetworkId",at=@At("RETURN"),cancellable=true,require=1)
    private void simulated$secondaryNetworkId(CallbackInfoReturnable<Long> result){
        result.setReturnValue(NativeKineticChannels.sourceId((GeneratingKineticBlockEntity)(Object)this,result.getReturnValue()));
    }
}
