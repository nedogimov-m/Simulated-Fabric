package dev.simulated_team.simulated.content.blocks.absorber;

import com.mojang.serialization.MapCodec;
import dev.simulated_team.simulated.fabric.block.NativeWrenchable;
import dev.simulated_team.simulated.fabric.block.NativeEntityBlock;
import dev.simulated_team.simulated.index.SimBlockShapes;
import dev.simulated_team.simulated.index.SimSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AbsorberBlock extends HorizontalDirectionalBlock implements NativeEntityBlock<AbsorberBlockEntity>, NativeWrenchable {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty WET = BooleanProperty.create("wet");
    public static final EnumProperty<Direction> HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;


    public AbsorberBlock(final Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(POWERED, false).setValue(WET,false));
    }

    @Override
    protected void neighborChanged(final BlockState state, final Level level, final BlockPos pos, final Block block,final net.minecraft.world.level.redstone.Orientation orientation, final boolean isMoving) {
        if (!level.isClientSide()) {
            final boolean flag = state.getValue(POWERED);
            if (flag != level.hasNeighborSignal(pos)) {
                level.setBlock(pos, state.cycle(POWERED), 2);
            }
        }
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return SimBlockShapes.EVAPORATOR;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HORIZONTAL_FACING, POWERED,WET);
        super.createBlockStateDefinition(builder);
    }

    @Override
    protected InteractionResult useItemOn(final ItemStack stack, final BlockState state, final Level level, final BlockPos pos, final Player player, final InteractionHand hand, final BlockHitResult hitResult) {
        if (stack.is(Items.CARROT) && state.getValue(POWERED)) {
            level.playLocalSound(pos, SoundEvents.GENERIC_EAT.value(), SoundSource.BLOCKS, 0.8f, 0.9f + 0.2f * level.getRandom().nextFloat(), false);
            level.playLocalSound(pos, SimSoundEvents.ABSORBER_EATS.event(), SoundSource.BLOCKS, 0.33f, 0.8f + 0.2f * level.getRandom().nextFloat(), false);

            if (level instanceof final ServerLevel serverLevel) {
                final Vec3 mouthPos = Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(state.getValue(FACING).getUnitVec3i()).scale(0.5));
                serverLevel.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(stack)), mouthPos.x, mouthPos.y, mouthPos.z, 5, 0, 0.1, 0, 0.01);
            }

            stack.consume(1, player);
            return InteractionResult.CONSUME;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext pContext) {
        final Direction dir = pContext.getHorizontalDirection().getOpposite();

        assert pContext.getPlayer() != null;
        return this.defaultBlockState().setValue(HORIZONTAL_FACING, pContext.getPlayer().isShiftKeyDown() ? dir.getOpposite() : dir);
    }

    @Override
    public Class<AbsorberBlockEntity> getBlockEntityClass() {
        return AbsorberBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AbsorberBlockEntity> getBlockEntityType() {
        //return SimBlockEntityTypes.ABSORBER;
        return null;
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

