package dev.ryanhcode.offroad.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.ryanhcode.offroad.content.blocks.borehead_bearing.BoreheadBearingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = RotationPropagator.class, remap = false)
public class RotationPropagatorMixin {

    @WrapOperation(method = "getGraphConnection", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;propagateRotationTo(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;ZZ)F"), require = 1)
    private static float offroad$boreheadBearingRotation(final KineticBlockEntity from, final KineticBlockEntity to,
            final BlockState fromState, final BlockState toState, final BlockPos delta,
            final boolean shafts, final boolean cogs, final Operation<Float> original) {
        final float custom = original.call(from, to, fromState, toState, delta, shafts, cogs);
        if (custom != 0 || !shafts || !(from instanceof BoreheadBearingBlockEntity || to instanceof BoreheadBearingBlockEntity)) {
            return custom;
        }

        // Both native graph solving and source propagation consume this same fixed port polarity.
        final Direction direction = Direction.getNearest(delta, Direction.NORTH);
        final float fromModifier = from instanceof BoreheadBearingBlockEntity bearing
                ? bearing.handleAxisModification(direction) : 1;
        final float toModifier = to instanceof BoreheadBearingBlockEntity bearing
                ? bearing.handleAxisModification(direction.getOpposite()) : 1;
        return fromModifier / toModifier;
    }

}
