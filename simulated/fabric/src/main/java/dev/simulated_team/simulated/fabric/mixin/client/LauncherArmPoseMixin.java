package dev.simulated_team.simulated.fabric.mixin.client;

import dev.simulated_team.simulated.api.LauncherArmPoseItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep the original launcher holding pose through native avatar extraction. */
@Mixin(AvatarRenderer.class)
public abstract class LauncherArmPoseMixin {
    @Inject(method = "getArmPose(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/client/model/HumanoidModel$ArmPose;",
            at = @At("HEAD"), cancellable = true)
    private static void simulated$launcherPose(Avatar avatar, ItemStack stack, InteractionHand hand,
                                               CallbackInfoReturnable<HumanoidModel.ArmPose> cir) {
        if (stack.getItem() instanceof LauncherArmPoseItem) cir.setReturnValue(HumanoidModel.ArmPose.CROSSBOW_HOLD);
    }
}
