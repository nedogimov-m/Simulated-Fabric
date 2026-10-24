package dev.simulated_team.simulated.mixin.linked_controller_binding;

import com.simibubi.create.content.redstone.link.controller.LinkedControllerItem;
import dev.simulated_team.simulated.index.SimBlocks;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LinkedControllerItem.class)
public abstract class LinkedControllerItemMixin {
    @Inject(method="useBlockFirst",at=@At("HEAD"),cancellable=true,remap=false)
    private void simulated$bindReceiver(Player player,Level level,InteractionHand hand,BlockHitResult hit,CallbackInfoReturnable<InteractionResult> callback){
        var state=level.getBlockState(hit.getBlockPos());
        if(player.isShiftKeyDown()||(!state.is(SimBlocks.MODULATING_LINKED_RECEIVER)&&!state.is(SimBlocks.DIRECTIONAL_LINKED_RECEIVER)))return;
        if(level.isClientSide())LinkedControllerItem.CLIENT_ACTION.accept(LinkedControllerItem.ClientAction.BIND,hit.getBlockPos().immutable());
        player.getCooldowns().addCooldown(player.getItemInHand(hand),2);
        callback.setReturnValue(InteractionResult.SUCCESS);
    }
}
