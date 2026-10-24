package dev.simulated_team.simulated.mixin.nav_table_compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// todo: wait on create to fix this
@Mixin(RedstoneLinkBlock.class)
public class RedstoneLinkBlockMixin {

    @WrapOperation(method = "updateTransmittedSignal", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/core/Direction;values()[Lnet/minecraft/core/Direction;"))
    private Direction[] fixReadPower(final Operation<Direction[]> original) {
        return new Direction[0]; // do not read the signal sent downwards for all adjacent blocks!!!
    }

}
