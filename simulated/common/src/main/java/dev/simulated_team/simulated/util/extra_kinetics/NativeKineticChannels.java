package dev.simulated_team.simulated.util.extra_kinetics;

import com.mojang.serialization.Codec;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Real secondary owners, with a persistent source-ID namespace outside valid vanilla world coordinates. */
public final class NativeKineticChannels {
    // Native physical network IDs encode X in the upper 26 bits. X=33554431 is outside Minecraft's
    // valid +/-30000000 block interval. These opaque IDs never become world positions or chunk queries.
    private static final long PREFIX = 33554431L << 38;
    private static final long COUNTER_MASK = (1L << 38)-1;
    private static final AttachmentType<Long> NEXT_ID = AttachmentRegistry.<Long>create(
            Identifier.parse("simulated:next_secondary_kinetic_network"), builder -> builder.initializer(()->0L).persistent(Codec.LONG));
    private NativeKineticChannels() {}
    public static AttachmentType<Long> bootstrap() { return NEXT_ID; }
    public static BlockEntity resolve(BlockPos requested,BlockEntity physical){
        if(!(requested instanceof ExtraBlockPos))return physical;
        if(physical instanceof ExtraKinetics owner)return owner.getExtraKinetics();
        return physical instanceof ExtraKinetics.ExtraKineticsBlockEntity?physical:null;
    }
    public static IRotate configuration(IRotate ordinary,BlockPos position,BlockState state){
        return position instanceof ExtraBlockPos && state.getBlock() instanceof ExtraKinetics.ExtraKineticsBlock owner
                ?owner.getExtraKineticsRotationConfiguration():ordinary;
    }
    public static IRotate configuration(KineticBlockEntity owner){
        return owner.getBlockState().getBlock() instanceof IRotate ordinary
                ?configuration(ordinary,owner.getBlockPos(),owner.getBlockState()):null;
    }
    public static long sourceId(KineticBlockEntity owner,long physicalId){
        if(!(owner.getBlockPos() instanceof ExtraBlockPos)||!(owner.getLevel() instanceof ServerLevel level))return physicalId;
        final var state=(NativeKineticChannelState)owner;
        final var target=(AttachmentTarget)level;
        synchronized(target){
            long cached=state.simulated$virtualNetworkId();
            long next=target.getAttachedOrElse(NEXT_ID,0L);
            if((cached&~COUNTER_MASK)==PREFIX){
                long after=(cached&COUNTER_MASK)+1;
                if(after>next)target.setAttached(NEXT_ID,after);
                return cached;
            }
            if(next>COUNTER_MASK)throw new IllegalStateException("Secondary kinetic network namespace exhausted");
            cached=PREFIX|next;
            target.setAttached(NEXT_ID,next+1);
            state.simulated$virtualNetworkId(cached);
            owner.setChanged();
            if(owner instanceof ExtraKinetics.ExtraKineticsBlockEntity extra)extra.getParentBlockEntity().setChanged();
            return cached;
        }
    }
}
