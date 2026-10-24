package dev.eriksonn.aeronautics.fabric.mixin;

import dev.eriksonn.aeronautics.fabric.fluid.LevititeFluidPhysics;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityFluidInteraction;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import java.util.*;

@Mixin(Entity.class)
abstract class LevititeEntityMixin {
    @Shadow @Final private EntityFluidInteraction fluidInteraction;
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityFluidInteraction;<init>(Ljava/util/Set;)V"), index = 0)
    private Set<TagKey<Fluid>> aeronautics$trackBlend(Set<TagKey<Fluid>> original) {
        final var tags = new HashSet<>(original); tags.add(LevititeFluidPhysics.BLEND); return Set.copyOf(tags);
    }
    @Inject(method = "updateFluidInteraction", at = @At("RETURN"))
    private void aeronautics$blendCurrent(CallbackInfoReturnable<Boolean> callback) {
        final var entity = (Entity) (Object) this;
        if (entity.isPushedByFluid()) fluidInteraction.applyCurrentTo(LevititeFluidPhysics.BLEND, entity, LevititeFluidPhysics.CURRENT_SCALE);
    }
    @Inject(method = "isInLiquid", at = @At("RETURN"), cancellable = true)
    private void aeronautics$inBlend(CallbackInfoReturnable<Boolean> callback) {
        if (fluidInteraction.isInFluid(LevititeFluidPhysics.BLEND)) callback.setReturnValue(true);
    }
    @Inject(method = "updateSwimming", at = @At("HEAD"), cancellable = true)
    private void aeronautics$cannotSwimInBlend(CallbackInfo callback) {
        if (!fluidInteraction.isInFluid(LevititeFluidPhysics.BLEND)) return;
        ((Entity) (Object) this).setSwimming(false);
        callback.cancel();
    }
}
