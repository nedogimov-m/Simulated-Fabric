package dev.simulated_team.simulated.fabric.block;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Native wrench dispatch preserves custom addon rotations, pickups and cancellation events. */
public interface NativeWrenchable {
    final class Dispatch { private static boolean registered; private Dispatch() {} }
    static void register() {
        if (Dispatch.registered) return;
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!player.getItemInHand(hand).is(AllItems.WRENCH) || player.isSpectator()
                    || !player.mayBuild() || !level.mayInteract(player, hit.getBlockPos())) return InteractionResult.PASS;
            final BlockState state = level.getBlockState(hit.getBlockPos());
            if (!(state.getBlock() instanceof NativeWrenchable wrenchable)) return InteractionResult.PASS;
            final var context = new UseOnContext(player, hand, hit);
            return player.isShiftKeyDown() ? wrenchable.onSneakWrenched(state, context) : wrenchable.onWrenched(state, context);
        });
        Dispatch.registered = true;
    }

    default InteractionResult onWrenched(BlockState state, UseOnContext context) {
        final Level level = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        final BlockState rotated = this.getRotatedBlockState(state, context.getClickedFace());
        if (!rotated.canSurvive(level, pos)) return InteractionResult.PASS;
        KineticBlockEntity.switchToBlockState(level, pos, this.updateAfterWrenched(rotated, context));
        if (level.getBlockState(pos) != state) playRotateSound(level, pos);
        return InteractionResult.SUCCESS;
    }
    default BlockState updateAfterWrenched(BlockState state, UseOnContext context) {
        return Block.updateFromNeighbourShapes(state, context.getLevel(), context.getClickedPos());
    }
    default InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        final BlockPos pos = context.getClickedPos();
        final var player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        final var entity = level.getBlockEntity(pos);
        if (!PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, pos, state, entity)) return InteractionResult.SUCCESS;
        if (!player.isCreative()) {
            Block.getDrops(state, level, pos, entity, player, context.getItemInHand()).forEach(
                    stack -> player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY));
        }
        state.spawnAfterBreak(level, pos, ItemStack.EMPTY, true);
        level.destroyBlock(pos, false);
        PlayerBlockBreakEvents.AFTER.invoker().afterBlockBreak(level, player, pos, state, entity);
        playRemoveSound(level, pos);
        return InteractionResult.SUCCESS;
    }
    static void playRotateSound(Level level, BlockPos pos) {
        level.playSound(null, pos, AllSoundEvents.WRENCH_ROTATE, SoundSource.BLOCKS, 1, level.getRandom().nextFloat() + .5f);
    }
    static void playRemoveSound(Level level, BlockPos pos) {
        level.playSound(null, pos, AllSoundEvents.WRENCH_REMOVE, SoundSource.BLOCKS, 1, level.getRandom().nextFloat() * .5f + .5f);
    }
    default BlockState getRotatedBlockState(BlockState state, Direction face) {
        if (face.getAxis() == Direction.Axis.Y) {
            if (state.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)) {
                return state.setValue(BlockStateProperties.HORIZONTAL_AXIS,
                        axisFace(state.getValue(BlockStateProperties.HORIZONTAL_AXIS)).getClockWise(face.getAxis()).getAxis());
            }
            if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                return state.setValue(BlockStateProperties.HORIZONTAL_FACING,
                        state.getValue(BlockStateProperties.HORIZONTAL_FACING).getClockWise(face.getAxis()));
            }
        }
        if (state.hasProperty(BlockStateProperties.AXIS)) {
            return state.setValue(BlockStateProperties.AXIS,
                    axisFace(state.getValue(BlockStateProperties.AXIS)).getClockWise(face.getAxis()).getAxis());
        }
        if (!state.hasProperty(BlockStateProperties.FACING)) return state;
        if (state.getValue(BlockStateProperties.FACING).getAxis() == face.getAxis()) {
            return state.hasProperty(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE)
                    ? state.cycle(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE) : state;
        }
        BlockState rotated = state;
        do {
            rotated = rotated.setValue(BlockStateProperties.FACING, rotated.getValue(BlockStateProperties.FACING).getClockWise(face.getAxis()));
            if (face.getAxis() == Direction.Axis.Y && rotated.hasProperty(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE)) {
                rotated = rotated.cycle(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE);
            }
        } while (rotated.getValue(BlockStateProperties.FACING).getAxis() == face.getAxis());
        return rotated;
    }
    private static Direction axisFace(Direction.Axis axis) { return Direction.get(Direction.AxisDirection.POSITIVE, axis); }
}
