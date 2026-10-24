package dev.eriksonn.aeronautics.fabric.mixin;

import dev.eriksonn.aeronautics.fabric.fluid.LevititeFluidPhysics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LiquidBlock.class)
abstract class LevititeLiquidBlockMixin {
    @Inject(method = "onPlace", at = @At("TAIL"))
    private void aeronautics$placed(BlockState state, Level level, BlockPos pos, BlockState previous, boolean moved, CallbackInfo callback) {
        LevititeFluidPhysics.reactLava(level, pos);
    }
    @Inject(method = "neighborChanged", at = @At("TAIL"))
    private void aeronautics$neighbor(BlockState state, Level level, BlockPos pos, Block block, Orientation orientation, boolean moved, CallbackInfo callback) {
        LevititeFluidPhysics.reactLava(level, pos);
    }
}
