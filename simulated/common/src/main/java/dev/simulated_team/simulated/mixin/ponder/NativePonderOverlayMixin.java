package dev.simulated_team.simulated.mixin.ponder;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.simulated_team.simulated.mixin_interface.ponder.NativePonderTextExtension;
import net.createmod.ponder.foundation.render.PonderSceneFrame;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Preserve scene positioning while suppressing only the pointer draw branch and its extra gap. */
@Mixin(targets = "net.createmod.ponder.foundation.ui.PonderOverlayRenderer", remap = false)
public class NativePonderOverlayMixin {
    @WrapOperation(method = "extract", at = @At(value = "INVOKE", target = "Lnet/createmod/ponder/foundation/render/PonderSceneFrame$TextOverlay;target()Lnet/minecraft/world/phys/Vec3;", ordinal = 2))
    private static Vec3 simulated$pointerTarget(PonderSceneFrame.TextOverlay text, Operation<Vec3> original) {
        return ((NativePonderTextExtension) (Object) text).simulated$hidePointer() ? null : original.call(text);
    }
    @WrapOperation(method = "extract", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(FF)F", ordinal = 0))
    private static float simulated$closePointerGap(float a, float b, Operation<Float> original, @Local PonderSceneFrame.TextOverlay text) {
        return original.call(a, ((NativePonderTextExtension) (Object) text).simulated$hidePointer() ? b - 50 : b);
    }
}
