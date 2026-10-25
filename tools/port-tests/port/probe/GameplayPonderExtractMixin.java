package port.probe;

import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.ui.PonderViewport;
import port.observation.GameplayPonder;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PonderViewport.class, remap = false)
public abstract class GameplayPonderExtractMixin {
    @Inject(method = "extract", at = @At("RETURN"))
    private static void create$observeRealExtraction(GuiGraphicsExtractor gui, PonderScene scene,
        int x, int y, int width, int height, double slide, float partial, float flash,
        CallbackInfoReturnable<PonderViewport.State> callback) {
        GameplayPonder.extracted(scene, callback.getReturnValue());
    }
}
