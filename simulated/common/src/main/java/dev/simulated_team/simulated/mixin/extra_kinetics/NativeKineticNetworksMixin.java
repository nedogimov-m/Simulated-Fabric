package dev.simulated_team.simulated.mixin.extra_kinetics;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.KineticNetworks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.utility.LoadedLevelAccess;
import dev.simulated_team.simulated.util.extra_kinetics.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.*;

/** Real Create solving, source stress maps and restoration stay authoritative for both stored channels. */
@Mixin(value=KineticNetworks.class,remap=false)
public abstract class NativeKineticNetworksMixin {
    @WrapOperation(method="tick",at=@At(value="NEW",target="()Ljava/util/HashMap;"),require=1)
    private static <K,V> HashMap<K,V> simulated$positionMaps(Operation<HashMap<K,V>> original){return new NativeKineticMap<>();}
    @WrapOperation(method="discoverLoadedOwners",at=@At(value="NEW",target="(Ljava/util/Collection;)Ljava/util/HashSet;"),require=1)
    private static <E> HashSet<E> simulated$discoverySet(Collection<? extends E> initial,Operation<HashSet<E>> original){return new NativeKineticSet<>(initial);}
    @WrapOperation(method="*",at=@At(value="INVOKE",target="Lnet/minecraft/core/BlockPos;equals(Ljava/lang/Object;)Z"),require=1)
    private static boolean simulated$sameChannel(BlockPos position,Object other,Operation<Boolean> original){return NativeKineticPositions.same(position,other);}
    @Inject(method="track",at=@At("TAIL"),require=1)
    private static void simulated$trackStoredSecondary(ServerLevel level,KineticBlockEntity owner,CallbackInfo callback){
        if(owner instanceof ExtraKinetics parent && parent.getExtraKinetics()!=null && parent.getExtraKinetics()!=owner)
            KineticNetworks.track(level,parent.getExtraKinetics());
    }
    @WrapOperation(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/core/BlockPos;asLong()J"),require=1)
    private static long simulated$secondarySourceId(BlockPos position,Operation<Long> original,@Local(argsOnly=true) ServerLevel level){
        long physical=original.call(position);
        return position instanceof ExtraBlockPos && LoadedLevelAccess.blockEntity(level,position) instanceof KineticBlockEntity owner
                ?NativeKineticChannels.sourceId(owner,physical):physical;
    }
    @ModifyVariable(method="tryDestroyForConflict",at=@At("HEAD"),argsOnly=true,require=1)
    private static KineticBlockEntity simulated$restoreActualPaidOwner(KineticBlockEntity owner){
        return owner instanceof ExtraKinetics.ExtraKineticsBlockEntity secondary?secondary.getParentBlockEntity():owner;
    }
}
