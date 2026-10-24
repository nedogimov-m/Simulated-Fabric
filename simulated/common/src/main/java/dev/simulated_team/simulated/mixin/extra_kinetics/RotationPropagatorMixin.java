package dev.simulated_team.simulated.mixin.extra_kinetics;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.utility.LoadedLevelAccess;
import dev.simulated_team.simulated.util.extra_kinetics.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.*;

/** Feed real secondary rotation configurations through the actual native Create connection rules. */
@Mixin(value=RotationPropagator.class,remap=false)
public abstract class RotationPropagatorMixin {
    private static final String RULES="getRotationSpeedModifier(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)D";
    private static final String OWNER_RULES="getRotationSpeedModifier(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;)F";

    @ModifyVariable(method=RULES,at=@At("STORE"),ordinal=0,require=1)
    private static IRotate simulated$fromConfiguration(IRotate ordinary,@Local(argsOnly=true,ordinal=0) BlockPos position,@Local(argsOnly=true,ordinal=0) BlockState state){
        return NativeKineticChannels.configuration(ordinary,position,state);
    }
    @ModifyVariable(method=RULES,at=@At("STORE"),ordinal=1,require=1)
    private static IRotate simulated$toConfiguration(IRotate ordinary,@Local(argsOnly=true,ordinal=1) BlockPos position,@Local(argsOnly=true,ordinal=1) BlockState state){
        return NativeKineticChannels.configuration(ordinary,position,state);
    }
    @ModifyVariable(method={"getGraphConnection",OWNER_RULES},at=@At("STORE"),ordinal=0,require=1)
    private static IRotate simulated$fromOwnerConfiguration(IRotate ordinary,@Local(argsOnly=true,ordinal=0) KineticBlockEntity owner){
        return NativeKineticChannels.configuration(ordinary,owner.getBlockPos(),owner.getBlockState());
    }
    @ModifyVariable(method={"getGraphConnection",OWNER_RULES},at=@At("STORE"),ordinal=1,require=1)
    private static IRotate simulated$toOwnerConfiguration(IRotate ordinary,@Local(argsOnly=true,ordinal=1) KineticBlockEntity owner){
        return NativeKineticChannels.configuration(ordinary,owner.getBlockPos(),owner.getBlockState());
    }
    @ModifyVariable(method="getPotentialNeighbourLocations",at=@At("STORE"),ordinal=0,require=1)
    private static IRotate simulated$propagationConfiguration(IRotate ordinary,@Local(argsOnly=true) KineticBlockEntity owner){
        return NativeKineticChannels.configuration(ordinary,owner.getBlockPos(),owner.getBlockState());
    }
    @WrapOperation(method="getGraphConnection",at=@At(value="INVOKE",target="Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;propagateRotationTo(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;ZZ)F"),require=1)
    private static float simulated$nativeCogConfiguration(KineticBlockEntity from,KineticBlockEntity to,BlockState fromState,BlockState toState,BlockPos delta,boolean shafts,boolean cogs,Operation<Float> original){
        final var left=NativeKineticChannels.configuration(from);
        final var right=NativeKineticChannels.configuration(to);
        final boolean smallCogs=left instanceof ICogWheel a && !a.isLargeCog() && right instanceof ICogWheel b && !b.isLargeCog();
        return original.call(from,to,fromState,toState,delta,shafts,smallCogs);
    }
    @WrapOperation(method="*",at=@At(value="INVOKE",target="Lnet/minecraft/core/BlockPos;equals(Ljava/lang/Object;)Z"),require=1)
    private static boolean simulated$sameSourceChannel(BlockPos position,Object other,Operation<Boolean> original){return NativeKineticPositions.same(position,other);}

    @Inject(method="getPotentialNeighbourLocations",at=@At("RETURN"),cancellable=true,require=1)
    private static void simulated$allStoredCandidates(KineticBlockEntity owner,CallbackInfoReturnable<List<BlockPos>> result){
        final var level=owner.getLevel();
        if(level==null||!LoadedLevelAccess.isLoaded(level,owner.getBlockPos()))return;
        final var candidates=new NativeKineticSet<BlockPos>();
        for(var position:result.getReturnValue()){
            candidates.add(position);
            if(LoadedLevelAccess.blockState(level,position).getBlock() instanceof ExtraKinetics.ExtraKineticsBlock)
                candidates.add(new ExtraBlockPos(position));
        }
        if(owner instanceof ExtraKinetics parent && parent.shouldConnectExtraKinetics() && parent.getExtraKinetics()!=null)
            candidates.add(parent.getExtraKinetics().getBlockPos());
        if(owner instanceof ExtraKinetics.ExtraKineticsBlockEntity secondary && secondary.getParentBlockEntity() instanceof ExtraKinetics parent && parent.shouldConnectExtraKinetics())
            candidates.add(secondary.getParentBlockEntity().getBlockPos());
        // The native owner returns an immutable distinct list; the addon supplies a fresh real list.
        result.setReturnValue(new ArrayList<>(candidates));
    }
}
