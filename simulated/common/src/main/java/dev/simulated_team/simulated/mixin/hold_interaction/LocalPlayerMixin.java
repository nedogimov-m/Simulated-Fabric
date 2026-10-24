package dev.simulated_team.simulated.mixin.hold_interaction;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.simulated_team.simulated.fabric.client.NativeHoldInput;
import dev.simulated_team.simulated.util.hold_interaction.HoldInteractionManager;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @Inject(method="isShiftKeyDown",at=@At("RETURN"),cancellable=true)
    private void simulated$blockLocalCrouch(CallbackInfoReturnable<Boolean> result) {
        if(result.getReturnValue()&&!HoldInteractionManager.canCrouch())result.setReturnValue(false);
    }
    @WrapOperation(method="sendChanges",at=@At(value="FIELD",target="Lnet/minecraft/client/player/ClientInput;keyPresses:Lnet/minecraft/world/entity/player/Input;"))
    private Input simulated$nativeOutgoingInput(ClientInput owner,Operation<Input> original) {
        return NativeHoldInput.filter(original.call(owner),HoldInteractionManager.canCrouch());
    }
}
