package dev.simulated_team.simulated.mixin.extra_kinetics;

import com.simibubi.create.foundation.utility.LoadedLevelAccess;
import dev.simulated_team.simulated.util.extra_kinetics.NativeKineticChannels;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Resolve only after the genuine non-loading lookup has returned its physical stored owner. */
@Mixin(value=LoadedLevelAccess.class,remap=false)
public abstract class NativeLoadedKineticOwnerMixin {
    @Inject(method="blockEntity",at=@At("RETURN"),cancellable=true,require=1)
    private static void simulated$secondaryOwner(BlockGetter level,BlockPos position,CallbackInfoReturnable<BlockEntity> result){
        result.setReturnValue(NativeKineticChannels.resolve(position,result.getReturnValue()));
    }
}
