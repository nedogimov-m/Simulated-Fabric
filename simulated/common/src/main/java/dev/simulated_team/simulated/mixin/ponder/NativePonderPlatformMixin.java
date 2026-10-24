package dev.simulated_team.simulated.mixin.ponder;

import dev.simulated_team.simulated.fabric.client.gui.NativePonderShadowSnapshot;
import dev.simulated_team.simulated.mixin_interface.ponder.PonderPlatformExtension;
import net.createmod.ponder.foundation.ui.PonderViewport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value=PonderViewport.Platform.class,remap=false)
public abstract class NativePonderPlatformMixin implements PonderPlatformExtension {
    @Unique private NativePonderShadowSnapshot simulated$shadow=NativePonderShadowSnapshot.DEFAULT;
    @Override public NativePonderShadowSnapshot simulated$getShadow(){return simulated$shadow;}
    @Override public void simulated$setShadow(NativePonderShadowSnapshot shadow){simulated$shadow=shadow;}
}
