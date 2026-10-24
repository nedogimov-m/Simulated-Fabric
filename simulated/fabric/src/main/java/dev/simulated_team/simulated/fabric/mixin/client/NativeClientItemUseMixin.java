package dev.simulated_team.simulated.fabric.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.simulated_team.simulated.events.SimulatedCommonClientEvents;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The original ITEM_AFTER_BLOCK hook runs only when native block interaction reaches item use. */
@Mixin(MultiPlayerGameMode.class)
public abstract class NativeClientItemUseMixin {
    @WrapOperation(method="performUseItemOn",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStack;useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult simulated$afterBlockUse(ItemStack stack,UseOnContext context,Operation<InteractionResult> original) {
        final var player=context.getPlayer();
        if(player!=null&&SimulatedCommonClientEvents.useItemOnBlockEvent(context.getLevel(),player,stack,context.getHand()))return InteractionResult.CONSUME;
        return original.call(stack,context);
    }
}
