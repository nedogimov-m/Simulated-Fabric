package dev.simulated_team.simulated.util.placement_helpers;

import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import dev.simulated_team.simulated.api.AssistedPlacement.Helper;
import dev.simulated_team.simulated.api.AssistedPlacement.Offset;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;
import java.util.function.Predicate;

public class CogwheelPlacementExtension extends SimplePlacementHelper {

    public CogwheelPlacementExtension(final Predicate<ItemStack> itemPredicate, final Predicate<BlockState> statePredicate) {
        super(itemPredicate, statePredicate);
    }

    @Override
    public Offset getOffset(final Player player, final Level world, final BlockState state, final BlockPos pos, final BlockHitResult ray) {
        final ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
        return this.getOffset(player, world, state, pos, ray, main.isEmpty() ? player.getItemInHand(InteractionHand.OFF_HAND) : main);
    }

    @Override
    public Offset getOffset(final Player player, final Level world, final BlockState state, final BlockPos pos,
                            final BlockHitResult ray, final ItemStack heldItem) {

        final Direction.Axis facingAxis;
        if (state.hasProperty(BlockStateProperties.AXIS)) {
            facingAxis = state.getValue(BlockStateProperties.AXIS);
        } else if (state.hasProperty(BlockStateProperties.FACING)) {
            facingAxis = state.getValue(BlockStateProperties.FACING).getAxis();
        } else {
            return Offset.fail();
        }

        //small cog
        if (ICogWheel.isSmallCogItem(heldItem)) {
            final List<Direction> validDirections = Helper.orderedByDistanceExceptAxis(pos, ray.getLocation(), facingAxis);
            for (final Direction dir : validDirections) {
                final BlockPos newPos = pos.relative(dir);

                if (!CogWheelBlock.isValidCogwheelPosition(false, world, newPos, facingAxis))
                    continue;

                if (!world.getBlockState(newPos)
                        .canBeReplaced())
                    continue;

                return Offset.success(newPos, s -> s.setValue(CogWheelBlock.AXIS, facingAxis));
            }

        } else {
            //large cog
            final Direction closest = Helper.orderedByDistanceExceptAxis(pos, ray.getLocation(), facingAxis)
                    .get(0);
            final List<Direction> directions = Helper.orderedByDistanceExceptAxis(pos, ray.getLocation(), facingAxis,
                    d -> d.getAxis() != closest.getAxis());

            for (final Direction dir : directions) {
                final BlockPos newPos = pos.relative(dir)
                        .relative(closest);
                if (!world.getBlockState(newPos)
                        .canBeReplaced())
                    continue;

                if (!CogWheelBlock.isValidCogwheelPosition(ICogWheel.isLargeCog(state), world, newPos, facingAxis))
                    continue;

                return Offset.success(newPos, s -> s.setValue(CogWheelBlock.AXIS, facingAxis));
            }
        }

        return Offset.fail();
    }
}
