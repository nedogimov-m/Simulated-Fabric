package port.probe;

import com.mojang.blaze3d.vertex.PoseStack;
import net.createmod.ponder.foundation.ui.PonderViewport;
import port.observation.GameplayPonder;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PonderViewport.Renderer.class, remap = false)
public abstract class GameplayPonderSubmitMixin {
    @Inject(method = "renderToTexture(Lnet/createmod/ponder/foundation/ui/PonderViewport$State;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V", at = @At("RETURN"))
    private void create$observeRealSubmission(PonderViewport.State state, PoseStack pose,
        SubmitNodeCollector collector, CallbackInfo callback) {
        GameplayPonder.submitted(state);
    }
}
