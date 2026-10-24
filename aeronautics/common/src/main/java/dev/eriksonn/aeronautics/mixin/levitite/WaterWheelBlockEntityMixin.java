package dev.eriksonn.aeronautics.mixin.levitite;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.waterwheel.WaterWheelBlockEntity;
import dev.eriksonn.aeronautics.service.AeroLevititeService;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WaterWheelBlockEntity.class)
public class WaterWheelBlockEntityMixin {

    @WrapOperation(method = "scoreAt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;getFlow(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 aeronautics$getFlowVectorAtPosition(FluidState fluid, BlockGetter level, BlockPos pos, Operation<Vec3> original) {
        Vec3 flow = original.call(fluid, level, pos);
        return fluid.getType().getBucket().equals(AeroLevititeService.INSTANCE.getBucket()) ? flow.reverse() : flow;
    }
}
