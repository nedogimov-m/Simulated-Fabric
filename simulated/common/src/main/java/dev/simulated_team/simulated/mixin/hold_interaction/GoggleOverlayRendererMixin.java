package dev.simulated_team.simulated.mixin.hold_interaction;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.equipment.goggles.GoggleOverlayRenderer;
import dev.simulated_team.simulated.index.SimClickInteractions;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GoggleOverlayRenderer.class)
public class GoggleOverlayRendererMixin {
    @Shadow public static int hoverTicks;

    @WrapOperation(method = "renderOverlay", at = @At(value = "INVOKE", target = "Ljava/lang/Math;clamp(FFF)F"), remap = false)
    private static float fixPartialTicks(final float value, final float min, final float max, final Operation<Float> original, @Local(argsOnly = true) final DeltaTracker deltaTracker) {
        if (SimClickInteractions.STEERING_WHEEL_MANAGER.isActive()) {
            hoverTicks = Math.clamp(hoverTicks - 2, 0, 24);
            return Math.clamp((hoverTicks - deltaTracker.getGameTimeDeltaPartialTick(false)) / 24, min, max);
        }
        return original.call(value, min, max);
    }

}
