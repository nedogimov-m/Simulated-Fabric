package dev.simulated_team.simulated.mixin.assembly;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.simulated_team.simulated.api.AssistedPlacement;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Apply helper state before the genuine native placement collision/survival checks. */
@Mixin(BlockItem.class)
public class AssistedBlockItemPlacementMixin {
    @WrapOperation(method = "getPlacementState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;getStateForPlacement(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState simulated$applyAssistedState(final Block block, final BlockPlaceContext context,
                                                   final Operation<BlockState> original) {
        return AssistedPlacement.transform(context, original.call(block, context));
    }
}
