package dev.simulated_team.simulated.mixin.extra_kinetics;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.utility.LoadedLevelAccess;
import dev.simulated_team.simulated.mixin_interface.extra_kinetics.KineticBlockEntityExtension;
import dev.simulated_team.simulated.util.extra_kinetics.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.*;

/** Preserve native owner lifecycle, graph invalidation and actual ValueInput/Output for the stored child. */
@Mixin(value=KineticBlockEntity.class,remap=false)
public abstract class KineticBlockEntityMixin extends SmartBlockEntity implements KineticBlockEntityExtension, NativeKineticChannelState {
    @Shadow private int validationCountdown;
    @Shadow public BlockPos source;
    @Shadow private BlockPos generatingSource;
    @Unique private boolean simulated$extraKineticsConnected;
    @Unique private long simulated$secondaryNetworkId;
    protected KineticBlockEntityMixin(BlockEntityType<?> type,BlockPos pos,BlockState state){super(type,pos,state);}
    @Override public void simulated$setValidationCountdown(int value){validationCountdown=value;}
    @Override public void simulated$setConnectedToExtraKinetics(boolean value){simulated$extraKineticsConnected=value;}
    @Override public boolean simulated$getConnectedToExtraKinetics(){return simulated$extraKineticsConnected;}
    @Override public long simulated$virtualNetworkId(){return simulated$secondaryNetworkId;}
    @Override public void simulated$virtualNetworkId(long value){simulated$secondaryNetworkId=value;}

    @Inject(method="switchToBlockState",at=@At("HEAD"),require=1)
    private static void simulated$detachStoredBeforeStateChange(Level level,BlockPos pos,BlockState next,CallbackInfo callback){
        if(!(level instanceof net.minecraft.server.level.ServerLevel)||!LoadedLevelAccess.isLoaded(level,pos)||LoadedLevelAccess.blockState(level,pos)==next)return;
        if(LoadedLevelAccess.blockEntity(level,pos) instanceof ExtraKinetics owner){
            final var extra=owner.getExtraKinetics();
            if(extra!=null){extra.detachKinetics();extra.removeSource();if(extra instanceof GeneratingKineticBlockEntity generator)generator.reActivateSource=true;}
        }
    }
    @Inject(method="setKineticProvenance",at=@At("HEAD"),require=1)
    private void simulated$sourceChannel(BlockPos generator,BlockPos upstream,net.minecraft.core.Direction face,CallbackInfo callback){
        simulated$extraKineticsConnected=upstream instanceof ExtraBlockPos;
    }
    @WrapOperation(method="setKineticProvenance",at=@At(value="INVOKE",target="Ljava/util/Objects;equals(Ljava/lang/Object;Ljava/lang/Object;)Z"),require=2)
    private boolean simulated$sourceIdentity(Object left,Object right,Operation<Boolean> original){
        return left instanceof BlockPos position && right instanceof BlockPos
                ?NativeKineticPositions.same(position,right):original.call(left,right);
    }
    @Override public void setLevel(Level level){
        super.setLevel(level);
        if(this instanceof ExtraKinetics owner && owner.getExtraKinetics()!=null)owner.getExtraKinetics().setLevel(level);
    }
    @Inject(method="setBlockState",at=@At("TAIL"),require=1)
    private void simulated$storedState(BlockState state,CallbackInfo callback){
        if(this instanceof ExtraKinetics owner && owner.getExtraKinetics()!=null)owner.getExtraKinetics().setBlockState(state);
    }
    @Inject(method="invalidate",at=@At("TAIL"),require=1)
    private void simulated$invalidateStored(CallbackInfo callback){
        if(this instanceof ExtraKinetics owner && owner.getExtraKinetics()!=null)owner.getExtraKinetics().invalidate();
    }
    @Inject(method="remove",at=@At("TAIL"),require=1)
    private void simulated$removeStored(CallbackInfo callback){
        if(this instanceof ExtraKinetics owner && owner.getExtraKinetics()!=null)owner.getExtraKinetics().remove();
    }
    @Inject(method="removeSource",at=@At("TAIL"),require=1)
    private void simulated$forgetSourceChannel(CallbackInfo callback){simulated$extraKineticsConnected=false;}
    @Inject(method="write",at=@At("TAIL"),require=1)
    private void simulated$saveStored(net.minecraft.world.level.storage.ValueOutput output,boolean clientPacket,CallbackInfo callback){
        if(this instanceof ExtraKinetics owner && owner.getExtraKinetics()!=null)
            ((NativeKineticDataAccessor)owner.getExtraKinetics()).simulated$writeKinetics(output.child(owner.getExtraKineticsSaveName()),clientPacket);
        output.putBoolean("ConnectedToExtraKinetics",simulated$extraKineticsConnected);
        output.putBoolean("GeneratorIsExtraKinetics",generatingSource instanceof ExtraBlockPos);
        if(simulated$secondaryNetworkId!=0)output.putLong("SimulatedSecondaryNetworkId",simulated$secondaryNetworkId);
    }
    @Inject(method="read",at=@At("TAIL"),require=1)
    private void simulated$readStored(net.minecraft.world.level.storage.ValueInput input,boolean clientPacket,CallbackInfo callback){
        if(this instanceof ExtraKinetics owner && owner.getExtraKinetics()!=null)
            ((NativeKineticDataAccessor)owner.getExtraKinetics()).simulated$readKinetics(input.childOrEmpty(owner.getExtraKineticsSaveName()),clientPacket);
        simulated$extraKineticsConnected=input.getBooleanOr("ConnectedToExtraKinetics",false);
        simulated$secondaryNetworkId=input.getLongOr("SimulatedSecondaryNetworkId",0L);
        if(simulated$extraKineticsConnected && source!=null)source=new ExtraBlockPos(source);
        if(input.getBooleanOr("GeneratorIsExtraKinetics",false) && generatingSource!=null)generatingSource=new ExtraBlockPos(generatingSource);
    }
}
