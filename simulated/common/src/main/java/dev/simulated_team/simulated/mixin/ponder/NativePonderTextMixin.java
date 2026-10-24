package dev.simulated_team.simulated.mixin.ponder;

import dev.simulated_team.simulated.mixin_interface.ponder.NativePonderTextExtension;
import net.createmod.ponder.foundation.render.PonderSceneFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PonderSceneFrame.TextOverlay.class)
public class NativePonderTextMixin implements NativePonderTextExtension {
    @Unique private boolean simulated$hidePointer;
    @Override public boolean simulated$hidePointer() { return this.simulated$hidePointer; }
    @Override public void simulated$setHidePointer(boolean hidden) { this.simulated$hidePointer = hidden; }
}
