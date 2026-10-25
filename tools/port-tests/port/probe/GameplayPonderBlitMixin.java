package port.probe;

import net.createmod.ponder.foundation.ui.PonderViewport;
import port.observation.GameplayPonder;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PictureInPictureRenderer.class, remap = false)
public abstract class GameplayPonderBlitMixin {
    @Inject(method = "blitTexture", at = @At("RETURN"))
    private void create$observeActualNativeBlit(PictureInPictureRenderState state,
        GuiRenderState gui, CallbackInfo callback) {
        if (state instanceof PonderViewport.State source) GameplayPonder.blitted(source);
    }
}
