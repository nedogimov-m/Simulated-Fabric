package dev.simulated_team.simulated.fabric.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.simulated_team.simulated.fabric.client.NativeGluePlotGeometry;
import net.createmod.catnip.outliner.AABBOutline;
import net.createmod.catnip.outliner.Outline;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native successor of the retired Sable Catnip AABB/chasing outline projection hooks. */
@Mixin(value = AABBOutline.class, remap = false)
public abstract class NativeGlueOutlinePlotMixin {
    @WrapOperation(method = "box", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;contains(Lnet/minecraft/world/phys/Vec3;)Z"))
    private boolean simulated$localCamera(AABB bounds, Vec3 camera, Operation<Boolean> original) {
        final var owner = NativeGluePlotGeometry.owner(bounds);
        return original.call(bounds, owner == null ? camera : owner.renderPose(NativeGluePlotGeometry.partial()).transformPositionInverse(camera));
    }
    @Inject(method = "box", at = @At("RETURN"), cancellable = true)
    private void simulated$projectOutline(AABB bounds, Vec3 camera, CallbackInfoReturnable<Outline.Geometry> cir) {
        cir.setReturnValue(NativeGluePlotGeometry.project(bounds, cir.getReturnValue()));
    }
}
