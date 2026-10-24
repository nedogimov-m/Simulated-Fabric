package dev.simulated_team.simulated.content.blocks.lasers.optical_sensor;

import com.simibubi.create.content.redstone.DirectedDirectionalBlock;
import dev.simulated_team.simulated.fabric.block.NativeEntityBlock;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.multiloader.CommonRedstoneBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public class OpticalSensorBlock extends DirectedDirectionalBlock implements NativeEntityBlock<OpticalSensorBlockEntity>, CommonRedstoneBlock  {

    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public OpticalSensorBlock(final Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
        super.createBlockStateDefinition(builder);
    }

    @Override
    protected InteractionResult useItemOn(final ItemStack stack, final BlockState state, final Level level, final BlockPos pos, final Player player, final InteractionHand hand, final BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof final OpticalSensorBlockEntity be) {
            if (be.tryApplyDye(stack)) {
                level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.DYE_USE, SoundSource.PLAYERS,
                        0.3f, 1.0f, false);
                return InteractionResult.SUCCESS;
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        BlockState state = this.defaultBlockState();
        Direction preferredFacing;

        final Direction facing = context.getNearestLookingDirection();
        preferredFacing = context.getPlayer() != null && context.getPlayer()
                .isSteppingCarefully() ? facing : facing.getOpposite();

        if (preferredFacing.getAxis() == Direction.Axis.Y) {
            state = state.setValue(TARGET, preferredFacing == Direction.UP ? AttachFace.CEILING : AttachFace.FLOOR);
            preferredFacing = context.getHorizontalDirection().getOpposite();
        }

        return state.setValue(FACING, preferredFacing).setValue(POWERED, false);
    }

    @Override
    public boolean commonConnectRedstone(final BlockState state, final BlockGetter world, final BlockPos pos, final Direction side) {
        return side != state.getValue(FACING)
                .getOpposite();
    }

    @Override
    public boolean isSignalSource(final BlockState state) {
        return state.getValue(POWERED);
    }

    @Override
    public boolean commonCheckWeakPower(final BlockState state, final SignalGetter level, final BlockPos pos, final Direction side) {
        return false;
    }

    @Override
    public int getSignal(final @NotNull BlockState blockState, final BlockGetter blockAccess, final BlockPos pos, final Direction side) {
        final OpticalSensorBlockEntity be = (OpticalSensorBlockEntity) blockAccess.getBlockEntity(pos);

        int power = 0;
        if (be != null && this.isSignalSource(blockState) && side != blockState.getValue(FACING).getOpposite()) {
            power = Math.round((be.getRaycastLength() - be.getRayDistance()) * (15f / be.getRaycastLength()));
        }

        return power;
    }

    @Override
    public Class<OpticalSensorBlockEntity> getBlockEntityClass() {
        return OpticalSensorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends OpticalSensorBlockEntity> getBlockEntityType() {
        return SimBlockEntityTypes.OPTICAL_SENSOR;
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        return NativeEntityBlock.create(this, pos, state);
    }

    @Override
    public <S extends net.minecraft.world.level.block.entity.BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<S> getTicker(
            net.minecraft.world.level.Level level, net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.block.entity.BlockEntityType<S> type) {
        return NativeEntityBlock.ticker(this, level, state, type);
    }
}
