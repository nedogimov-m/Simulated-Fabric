package dev.simulated_team.simulated.mixin.new_ponder;

import dev.simulated_team.simulated.ponder.new_ponder_tooltip.NewPonderTooltipManager;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.ui.PonderUI;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = PonderUI.class, remap = false)
public class PonderUIMixin {
    @Shadow @Final private List<PonderScene> scenes;
    @Shadow private int index;

    // Native scroll delegates to select; index is committed here before the new
    // scene begins, including selection through tags and navigation controls.
    @Inject(method = "select(I)Z", at = @At(value = "INVOKE", target = "Lnet/createmod/ponder/foundation/PonderScene;begin()V"))
    private void simulated$begin(final int next, final CallbackInfoReturnable<Boolean> cir) {
        NewPonderTooltipManager.setSceneWatched(this.scenes.get(this.index).getId());
    }
}
