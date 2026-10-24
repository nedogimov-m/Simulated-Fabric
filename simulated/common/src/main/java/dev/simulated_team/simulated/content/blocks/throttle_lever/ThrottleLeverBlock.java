package dev.simulated_team.simulated.content.blocks.throttle_lever;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllItems;
import dev.simulated_team.simulated.fabric.block.NativeWrenchable;
import dev.simulated_team.simulated.fabric.block.NativeEntityBlock;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.index.SimBlockShapes;
import dev.simulated_team.simulated.index.SimClickInteractions;
import dev.simulated_team.simulated.multiloader.CommonRedstoneBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class ThrottleLeverBlock extends FaceAttachedHorizontalDirectionalBlock implements NativeEntityBlock<ThrottleLeverBlockEntity>, NativeWrenchable, CommonRedstoneBlock {
    public static BooleanProperty INVERTED = BooleanProperty.create("inverted");

    public ThrottleLeverBlock(final Properties builder) {
        super(builder);
        this.registerDefaultState(this.defaultBlockState().setValue(INVERTED, false));
    }

    private static void addParticles(final BlockState state, final LevelAccessor level, final BlockPos pos, final float alpha) {
        final Direction direction = state.getValue(FACING)
                .getOpposite();
        final Direction direction1 = getConnectedDirection(state).getOpposite();
        final double d0 =
                (double) pos.getX() + 0.5D + 0.1D * (double) direction.getStepX() + 0.2D * (double) direction1.getStepX();
        final double d1 =
                (double) pos.getY() + 0.5D + 0.1D * (double) direction.getStepY() + 0.2D * (double) direction1.getStepY();
        final double d2 =
                (double) pos.getZ() + 0.5D + 0.1D * (double) direction.getStepZ() + 0.2D * (double) direction1.getStepZ();
        level.addParticle(new DustParticleOptions(0xff0000, alpha), d0, d1, d2, 0.0D, 0.0D,
                0.0D);
    }

    static void updateNeighbors(final BlockState state, final Level world, final BlockPos pos) {
        world.updateNeighborsAt(pos, state.getBlock());
        world.updateNeighborsAt(pos.relative(getConnectedDirection(state).getOpposite()), state.getBlock());
    }

    @Override
    protected void affectNeighborsAfterRemoval(final BlockState state, final net.minecraft.server.level.ServerLevel level,
                                              final BlockPos pos, final boolean isMoving) {
        if (!isMoving) updateNeighbors(state, level, pos);
        super.affectNeighborsAfterRemoval(state, level, pos, isMoving);
    }


    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult) {
        if (player.getMainHandItem().is(AllItems.WRENCH)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide() && player.isLocalPlayer()) {
            addParticles(state, level, pos, 1.0F);
            return this.onBlockEntityUse(level, pos, be -> {
                if (!SimClickInteractions.THROTTLE_LEVER_MANAGER.isActive()) {
                    SimClickInteractions.THROTTLE_LEVER_MANAGER.startHold(level, player, pos);
                    return InteractionResult.SUCCESS;
                }
                return InteractionResult.FAIL;
            });
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public boolean commonConnectRedstone(final BlockState state, final BlockGetter level, final BlockPos pos, @Nullable final Direction direction) {
        return direction != null;
    }

    @Override
    public InteractionResult onWrenched(final BlockState state, final UseOnContext context) {
        final Level level = context.getLevel();
        if (level.isClientSide())
            return InteractionResult.SUCCESS;
        final BlockPos pos = context.getClickedPos();
        final int signal = this.getSignal(state, level, pos, context.getClickedFace());

        addParticles(state, level, pos, 1.0F);
        level.setBlock(pos, state.cycle(INVERTED), 2);
        this.withBlockEntityDo(level, pos, (be) -> {
            be.setSignal(state.getValue(INVERTED) ? 15 - signal : signal);
        });
        return InteractionResult.SUCCESS;
    }

    @Override
    public int getSignal(final BlockState blockState, final BlockGetter blockAccess, final BlockPos pos, final Direction side) {
        return this.getBlockEntityOptional(blockAccess, pos).map(al -> al.state)
                .orElse(0);
    }

    @Override
    public boolean isSignalSource(final BlockState state) {
        return true;
    }

    @Override
    public int getDirectSignal(final BlockState blockState, final BlockGetter blockAccess, final BlockPos pos, final Direction side) {
        return getConnectedDirection(blockState) == side ? this.getSignal(blockState, blockAccess, pos, side) : 0;
    }

    @Override
    public void animateTick(final BlockState pState, final Level pLevel, final BlockPos pPos, final RandomSource pRandom) {
        this.withBlockEntityDo(pLevel, pPos, be -> {
            if (be.state != 0 && pRandom.nextFloat() < 0.25F)
                addParticles(pState, pLevel, pPos, 0.5F);
        });
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        if (state.getValue(FACE) != AttachFace.WALL && state.getValue(FACING).getAxis() == Direction.Axis.X)
            return SimBlockShapes.THROTTLE_LEVER_SWAP.get(getConnectedDirection(state));
        return SimBlockShapes.THROTTLE_LEVER.get(getConnectedDirection(state));
    }

    public VoxelShape getHandleShape(final BlockState state) {
        if (state.getValue(FACE) != AttachFace.WALL && state.getValue(FACING).getAxis() == Direction.Axis.X)
            return SimBlockShapes.THROTTLE_LEVER_HANDLE_SWAP.get(getConnectedDirection(state));
        return SimBlockShapes.THROTTLE_LEVER_HANDLE.get(getConnectedDirection(state));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(FACING, FACE, INVERTED));
    }

    @Override
    public Class<ThrottleLeverBlockEntity> getBlockEntityClass() {
        return ThrottleLeverBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ThrottleLeverBlockEntity> getBlockEntityType() {
        return SimBlockEntityTypes.THROTTLE_LEVER;
    }

    @Override
    protected boolean isPathfindable(final BlockState blockState, final PathComputationType pathComputationType) {
        return false;
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
