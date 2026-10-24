package dev.eriksonn.aeronautics.fabric.mixin;

import dev.eriksonn.aeronautics.fabric.fluid.LevititeFluidClient;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
abstract class LevititeFogMixin {
    @Inject(method = "setupFog", at = @At("RETURN"))
    private void aeronautics$fog(Camera camera, int distance, DeltaTracker delta, float darkness,
                                ClientLevel level, CallbackInfoReturnable<FogData> callback) {
        LevititeFluidClient.fog(camera, level, callback.getReturnValue());
    }
}
