package dev.simulated_team.simulated.mixin.ponder;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.createmod.ponder.foundation.element.WorldSectionElementImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Preserve Simulated's original X/Z/Y section and stabilization rotation order. */
@Mixin(WorldSectionElementImpl.class)
public class WorldSectionElementImplMixin {
    @WrapOperation(method = "transformMS", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;rotateDegrees(Lcom/mojang/math/Axis;F)V", ordinal = 1))
    private void simulated$rotateZ(PoseStack pose, Axis axis, float degrees, Operation<Void> original, @Local(ordinal = 1) double rotationZ) {
        original.call(pose, Axis.ZP, (float) rotationZ);
    }
    @WrapOperation(method = "transformMS", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;rotateDegrees(Lcom/mojang/math/Axis;F)V", ordinal = 2))
    private void simulated$rotateY(PoseStack pose, Axis axis, float degrees, Operation<Void> original, @Local(ordinal = 2) double rotationY) {
        original.call(pose, Axis.YP, (float) rotationY);
    }
    @WrapOperation(method = "transformMS", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;rotateDegrees(Lcom/mojang/math/Axis;F)V", ordinal = 4))
    private void simulated$stabilizeZ(PoseStack pose, Axis axis, float degrees, Operation<Void> original, @Local(ordinal = 1) double rotationZ) {
        original.call(pose, Axis.ZP, (float) -rotationZ);
    }
    @WrapOperation(method = "transformMS", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;rotateDegrees(Lcom/mojang/math/Axis;F)V", ordinal = 5))
    private void simulated$stabilizeY(PoseStack pose, Axis axis, float degrees, Operation<Void> original, @Local(ordinal = 2) double rotationY) {
        original.call(pose, Axis.YP, (float) -rotationY);
    }
}
