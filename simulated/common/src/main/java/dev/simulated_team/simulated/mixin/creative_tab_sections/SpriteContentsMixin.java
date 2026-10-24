package dev.simulated_team.simulated.mixin.creative_tab_sections;

import dev.simulated_team.simulated.mixin_interface.SpriteContentsExtension;
import dev.simulated_team.simulated.mixin_interface.TickerExtension;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpriteContents.class)
public class SpriteContentsMixin implements SpriteContentsExtension {
    @Unique private TickerExtension simulated$state;
    @Override public TickerExtension simulated$getTicker(){return simulated$state;}
    @Override public void simulated$setTicker(TickerExtension state){simulated$state=state;}
    @Inject(method="createAnimationState",at=@At("RETURN"))
    private void simulated$captureState(CallbackInfoReturnable<SpriteContents.AnimationState> result){
        simulated$state=result.getReturnValue() instanceof TickerExtension extension?extension:null;
    }
}
