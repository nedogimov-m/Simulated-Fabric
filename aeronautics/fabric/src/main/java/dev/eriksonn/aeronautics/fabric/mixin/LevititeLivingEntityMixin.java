package dev.eriksonn.aeronautics.fabric.mixin;

import dev.eriksonn.aeronautics.fabric.fluid.LevititeFluidPhysics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
abstract class LevititeLivingEntityMixin {
    @Shadow protected abstract double getEffectiveGravity();
    @Inject(method = "travelInFluid", at = @At("HEAD"), cancellable = true)
    private void aeronautics$levitatingFluid(Vec3 input, CallbackInfo callback) {
        final var entity = (LivingEntity) (Object) this;
        if (entity.getFluidHeight(LevititeFluidPhysics.BLEND) <= 0) return;
        LevititeFluidPhysics.travel(entity, input, getEffectiveGravity());
        callback.cancel();
    }
}
