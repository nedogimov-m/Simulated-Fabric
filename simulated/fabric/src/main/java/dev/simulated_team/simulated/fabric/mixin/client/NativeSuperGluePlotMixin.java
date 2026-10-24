package dev.simulated_team.simulated.fabric.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.contraptions.glue.SuperGlueSelectionHandler;
import dev.simulated_team.simulated.fabric.client.NativeGluePlotGeometry;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Optional;

/** Projects only plot-local native glue boxes; hover clips the world ray in the same plot. */
@Mixin(value = SuperGlueSelectionHandler.class, remap = false)
public abstract class NativeSuperGluePlotMixin {
    @Inject(method = "box", at = @At("HEAD"), cancellable = true)
    private static void simulated$plotBox(DrawableGizmoPrimitives output, AABB bounds, int color, float width, CallbackInfoReturnable<Integer> cir) {
        final var owner = NativeGluePlotGeometry.owner(bounds);
        if (owner != null) cir.setReturnValue(NativeGluePlotGeometry.box(output, bounds, color, width, owner));
    }
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;clip(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Ljava/util/Optional;"))
    private static Optional<Vec3> simulated$plotHover(AABB bounds, Vec3 origin, Vec3 target, Operation<Optional<Vec3>> original) {
        final var owner = NativeGluePlotGeometry.owner(bounds);
        if (owner == null) return original.call(bounds, origin, target);
        final var pose = owner.logicalPose();
        return original.call(bounds, pose.transformPositionInverse(origin), pose.transformPositionInverse(target)).map(pose::transformPosition);
    }
}
