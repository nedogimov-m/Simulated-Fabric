package dev.simulated_team.simulated.mixin.handle;

import dev.simulated_team.simulated.mixin_interface.handle.NativeHandlePoseExtension;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(HumanoidRenderState.class)
public class NativeHandlePoseStateMixin implements NativeHandlePoseExtension {
    @Unique private boolean simulated$holdingHandle;
    @Override public boolean simulated$holdingHandle() { return this.simulated$holdingHandle; }
    @Override public void simulated$holdingHandle(boolean holding) { this.simulated$holdingHandle = holding; }
}
