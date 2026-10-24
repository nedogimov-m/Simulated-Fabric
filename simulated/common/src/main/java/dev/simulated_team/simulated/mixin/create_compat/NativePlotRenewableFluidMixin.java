package dev.simulated_team.simulated.mixin.create_compat;

import com.simibubi.create.content.fluids.WorldFluidEdit;
import dev.ryanhcode.sable.Sable;
import dev.simulated_team.simulated.mixin.accessor.NativeFlowingFluidAccessor;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FlowingFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Restore upstream OpenEndedPipe.removeFluidFromSpace's renewal guard on plots. */
@Mixin(value = WorldFluidEdit.class, remap = false)
public abstract class NativePlotRenewableFluidMixin {
    @Inject(method = "stage", at = @At("HEAD"), cancellable = true)
    private static void simulated$preserveRenewableSource(
            Level level, BlockPos pos, BlockState replacement, boolean evaporate,
            TransactionContext transaction, CallbackInfo ci) {
        if (evaporate || !(level instanceof ServerLevel serverLevel)
                || !replacement.hasProperty(LiquidBlock.LEVEL)
                || replacement.getValue(LiquidBlock.LEVEL) != 14
                || Sable.HELPER.getContaining(level, pos) == null) return;

        BlockState original = WorldFluidEdit.state(level, pos);
        var originalFluid = original.getFluidState();
        if (!originalFluid.isSource() || !original.canBeReplaced()
                || original.hasProperty(BlockStateProperties.WATERLOGGED)
                || !replacement.equals(originalFluid.createLegacyBlock().setValue(LiquidBlock.LEVEL, 14))
                || !(replacement.getFluidState().getType() instanceof FlowingFluid flowing)) return;

        // Create mc1.21.1-6.0.10 skips the drain edit if the hypothetical flowing
        // state would immediately become the same source again. Ask native fluid
        // logic rather than assuming a particular fluid, neighbour count or rule.
        var renewed = ((NativeFlowingFluidAccessor) flowing)
                .simulated$getNewLiquid(serverLevel, pos, replacement);
        if (renewed.equals(originalFluid)) ci.cancel();
    }
}
