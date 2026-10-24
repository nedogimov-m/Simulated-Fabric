package dev.simulated_team.simulated.mixin.extra_kinetics;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.simulated_team.simulated.util.extra_kinetics.NativeKineticMap;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Map;

/** Both real stored kinetic owners remain independently indexed at the same physical coordinates. */
@Mixin(targets="com.simibubi.create.content.kinetics.KineticNetworks$LoadedKinetics",remap=false)
public abstract class NativeLoadedKineticIndexMixin {
    @Mutable @Shadow @Final private Map<BlockPos,KineticBlockEntity> members;
    @Mutable @Shadow @Final private Map<BlockPos,Double> sourceSpeeds;
    @Inject(method="<init>",at=@At("TAIL"),require=1)
    private void simulated$channelIndex(CallbackInfo callback){
        members=new NativeKineticMap<>(members);
        sourceSpeeds=new NativeKineticMap<>(sourceSpeeds);
    }
}
