package dev.simulated_team.simulated.fabric.mixin.client;

import dev.simulated_team.simulated.fabric.client.NativeSimWorldShaderBindings;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class NativeShaderFrameMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void simulated$prepareLighting(CallbackInfo ci) { NativeSimWorldShaderBindings.prepareFrame();
        dev.simulated_team.simulated.fabric.client.NativeEndSeaShaders.prepareFrame(); }
    @Inject(method = "close", at = @At("HEAD"))
    private void simulated$closeLighting(CallbackInfo ci) { NativeSimWorldShaderBindings.close(); }
}
