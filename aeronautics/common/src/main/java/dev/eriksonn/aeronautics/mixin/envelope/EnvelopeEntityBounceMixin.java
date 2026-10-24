package dev.eriksonn.aeronautics.mixin.envelope;

import dev.eriksonn.aeronautics.content.blocks.hot_air.envelope.EnvelopeEncasedShaftBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native nonliving bounce defaults to80%; the original encased envelope requires50% of its living bounce. */
@Mixin(Entity.class)
public abstract class EnvelopeEntityBounceMixin {
    @Inject(method="getBlockBounciness",at=@At("HEAD"),cancellable=true,require=1)
    private void aeronautics$encasedRestitution(Block block,CallbackInfoReturnable<Double> callback) {
        if(block instanceof EnvelopeEncasedShaftBlock && !((Object)this instanceof LivingEntity))callback.setReturnValue(.25);
    }
}
