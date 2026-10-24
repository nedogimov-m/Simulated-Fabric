package dev.simulated_team.simulated.mixin.handle;

import dev.simulated_team.simulated.content.blocks.handle.PlayerHoldingHandleRenderer;
import dev.simulated_team.simulated.mixin_interface.handle.NativeHandlePoseExtension;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public class HumanoidModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("RETURN"))
    private void simulated$afterSetupAnim(final HumanoidRenderState state, final CallbackInfo callbackInfo) {
        if (((NativeHandlePoseExtension) state).simulated$holdingHandle())
            PlayerHoldingHandleRenderer.setHangingPose((HumanoidModel<?>) (Object) this);
    }
}
