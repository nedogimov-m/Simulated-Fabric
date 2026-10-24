package dev.simulated_team.simulated.mixin.ponder;

import dev.simulated_team.simulated.mixin_interface.ponder.TextWindowElementExtension;
import dev.simulated_team.simulated.mixin_interface.ponder.NativePonderTextExtension;
import net.createmod.ponder.foundation.element.TextWindowElement;
import net.createmod.ponder.foundation.render.PonderSceneFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Copy pointer visibility into the actual immutable text snapshot before deferred GUI extraction. */
@Mixin(TextWindowElement.class)
public class TextWindowElementMixin implements TextWindowElementExtension {
    @Unique private boolean simulated$shouldHidePointer;
    @Override public void simulated$hidePointer() { this.simulated$shouldHidePointer = true; }
    @Inject(method = "snapshot", at = @At("RETURN"))
    private void simulated$capturePointer(float partial, CallbackInfoReturnable<PonderSceneFrame.TextOverlay> result) {
        ((NativePonderTextExtension) (Object) result.getReturnValue()).simulated$setHidePointer(this.simulated$shouldHidePointer);
    }
}
