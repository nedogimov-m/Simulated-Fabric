package dev.eriksonn.aeronautics.content.blocks.propeller.small.smart_propeller;

import dev.simulated_team.simulated.fabric.block.NativeWrenchable;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import dev.simulated_team.simulated.fabric.block.NativeEntityBlock;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.BasePropellerBlock;
import dev.eriksonn.aeronautics.index.AeroBlockEntityTypes;
import dev.eriksonn.aeronautics.index.AeroBlockShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SmartPropellerBlock extends HorizontalAxisKineticBlock implements NativeWrenchable, NativeEntityBlock<SmartPropellerBlockEntity> {
    public static final BooleanProperty REVERSED = BasePropellerBlock.REVERSED;
    public static final BooleanProperty CEILING = BooleanProperty.create("ceiling");

    public SmartPropellerBlock(final Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any().setValue(REVERSED, false).setValue(CEILING, false));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(REVERSED);
        builder.add(CEILING);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        final BlockState state = super.getStateForPlacement(context);
        final Direction.Axis axis = state.getValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS);
        return state
                .setValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS, axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X)
                .setValue(CEILING, context.getClickedFace() == Direction.DOWN);
    }

    @Override
    public Direction.Axis getRotationAxis(final BlockState state) {
        return Direction.Axis.Y;
    }

    @Override
    public boolean hasShaftTowards(final LevelReader world, final BlockPos pos, final BlockState state, final Direction face) {
        return state.getValue(CEILING) ? face == Direction.UP : face == Direction.DOWN;
    }

    @Override
    public InteractionResult onWrenched(BlockState state, final UseOnContext context) {
        if (context.getClickedFace() == (state.getValue(SmartPropellerBlock.CEILING) ? Direction.DOWN : Direction.UP)) {
            state = state.cycle(REVERSED);
            context.getLevel().setBlock(context.getClickedPos(), state, 3);
            NativeWrenchable.playRotateSound(context.getLevel(), context.getClickedPos());
            return InteractionResult.SUCCESS;
        }

        return NativeWrenchable.super.onWrenched(state, context);
    }

    @Override
    public VoxelShape getShape(final BlockState pState, final BlockGetter pLevel, final BlockPos pPos, final CollisionContext pContext) {
        if (pState.getValue(CEILING)) {
            return AeroBlockShapes.SMART_PROPELLER_CEILING.get(pState.getValue(HORIZONTAL_AXIS));
        }

        return AeroBlockShapes.SMART_PROPELLER.get(pState.getValue(HORIZONTAL_AXIS));
    }

    @Override
    public Class<SmartPropellerBlockEntity> getBlockEntityClass() {
        return SmartPropellerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SmartPropellerBlockEntity> getBlockEntityType() {
        return AeroBlockEntityTypes.SMART_PROPELLER;
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
