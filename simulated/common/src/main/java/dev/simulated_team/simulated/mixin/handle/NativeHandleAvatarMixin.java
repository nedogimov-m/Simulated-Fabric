package dev.simulated_team.simulated.mixin.handle;

import dev.simulated_team.simulated.content.blocks.handle.PlayerHoldingHandleRenderer;
import dev.simulated_team.simulated.mixin_interface.handle.NativeHandlePoseExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Capture live UUID/paused state before deferred native model preparation. */
@Mixin(AvatarRenderer.class)
public class NativeHandleAvatarMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
    private void simulated$captureHandle(Avatar avatar, AvatarRenderState state, float partial, CallbackInfo ci) {
        ((NativeHandlePoseExtension) state).simulated$holdingHandle(!Minecraft.getInstance().isPaused()
                && PlayerHoldingHandleRenderer.isHolding(avatar.getUUID()));
    }
}
