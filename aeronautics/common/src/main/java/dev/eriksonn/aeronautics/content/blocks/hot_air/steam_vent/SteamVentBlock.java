package dev.eriksonn.aeronautics.content.blocks.hot_air.steam_vent;

import dev.simulated_team.simulated.fabric.block.NativeWrenchable;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import dev.simulated_team.simulated.fabric.block.NativeEntityBlock;
import dev.eriksonn.aeronautics.index.AeroBlockEntityTypes;
import dev.eriksonn.aeronautics.index.AeroBlockShapes;
import dev.eriksonn.aeronautics.index.AeroTags;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;

public class SteamVentBlock extends Block implements NativeEntityBlock<SteamVentBlockEntity>, SimpleWaterloggedBlock, NativeWrenchable {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<Variant> VARIANT = EnumProperty.create("variant", Variant.class);


    public SteamVentBlock(final Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, false).setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(POWERED, WATERLOGGED, VARIANT, FACING));
    }

    @Override
    protected @NotNull InteractionResult useItemOn(final ItemStack itemStack,
                                                       final @NotNull BlockState blockState,
                                                       final @NotNull Level level,
                                                       final @NotNull BlockPos blockPos,
                                                       final @NotNull Player player,
                                                       final @NotNull InteractionHand interactionHand,
                                                       final @NotNull BlockHitResult blockHitResult) {
        final Variant conversion = Variant.getConversionFromItem(itemStack.getItem());

        if (conversion != null) {
            final Variant current = blockState.getValue(VARIANT);
            if (conversion != current) {
                level.setBlockAndUpdate(blockPos, blockState.setValue(VARIANT, conversion));
                level.playLocalSound(blockPos.getX(), blockPos.getY(), blockPos.getZ(), SoundEvents.COPPER_PLACE, SoundSource.BLOCKS, 1, 1, false);
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    public void onPlace(final @NotNull BlockState pState, final @NotNull Level pLevel, final BlockPos pPos, final @NotNull BlockState pOldState, final boolean pIsMoving) {
        updateBoiler(pLevel, pPos.below());

        this.withBlockEntityDo(pLevel, pPos, SteamVentBlockEntity::getAndCacheTank);
        this.withBlockEntityDo(pLevel, pPos, x -> {
            if (!x.updateRawSignal()) {
                x.signalSync();
            }
        });
    }

    @Override
    protected void affectNeighborsAfterRemoval(final BlockState state, final net.minecraft.server.level.ServerLevel level, final BlockPos pos, final boolean moving) {
        for (Direction direction : Iterate.directions)
            if (level.getBlockEntity(pos.relative(direction)) instanceof SteamVentBlockEntity vent) vent.signalSync();
        updateBoiler(level, pos.below());
        super.affectNeighborsAfterRemoval(state, level, pos, moving);
    }

    private static void updateBoiler(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof com.simibubi.create.content.fluids.tank.FluidTankBlockEntity tank) {
            tank.updateBoilerState(); tank.updateBoilerTemperature();
        }
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        final Level level = context.getLevel();
        final BlockPos pos = context.getClickedPos();


        return super.getStateForPlacement(context)
                .setValue(POWERED, level.hasNeighborSignal(pos))
                .setValue(WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER)
                .setValue(FACING, context.getPlayer().isShiftKeyDown() ? context.getHorizontalDirection().getOpposite() : context.getHorizontalDirection());
    }

    @Override
    public void neighborChanged(final BlockState state, final Level level, final BlockPos pos, final Block blockIn, final net.minecraft.world.level.redstone.Orientation orientation, final boolean isMoving) {
        if (level.isClientSide())
            return;

        this.withBlockEntityDo(level, pos, SteamVentBlockEntity::updateRawSignal);

    }

    @Override
    public boolean canSurvive(final BlockState pState, final LevelReader pLevel, final BlockPos pPos) {
        return SteamEngineBlock.canAttach(pLevel, pPos, Direction.DOWN);
    }

    @Override
    public VoxelShape getShape(final BlockState pState, final BlockGetter pLevel, final BlockPos pPos, final CollisionContext pContext) {
        return AeroBlockShapes.STEAM_VENT.get(Direction.Axis.Y);
    }

    @Override
    public FluidState getFluidState(final BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
    }

    @Override
    public Class<SteamVentBlockEntity> getBlockEntityClass() {
        return SteamVentBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SteamVentBlockEntity> getBlockEntityType() {
        return AeroBlockEntityTypes.STEAM_VENT;
    }

    public enum Variant implements StringRepresentable {
        GOLD,
        IRON;

        public static Variant getConversionFromItem(final Item item) {
            //todo: these should probably be tags somehow
            if (item.builtInRegistryHolder().is(AeroTags.ItemTags.GOLD_SHEET)) return GOLD;
            if (item.builtInRegistryHolder().is(AeroTags.ItemTags.IRON_SHEET)) return IRON;
            return null;
        }

        @Override
        public String getSerializedName() {
            return this.toString().toLowerCase(Locale.ROOT);
        }
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
