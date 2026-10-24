package dev.simulated_team.simulated.fabric.mixin.client;

import dev.simulated_team.simulated.fabric.client.NativeEndSeaShaders;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public final class NativeEndSeaFrameMixin {
    @Shadow @Final private LevelRenderState levelRenderState;
    @Inject(method="render",at=@At("HEAD"))
    private void simulated$prepareEndSeaWorld(CallbackInfo callback){NativeEndSeaShaders.prepareWorld(levelRenderState);}
}
