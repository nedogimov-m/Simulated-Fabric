package dev.ryanhcode.offroad.content.blocks.borehead_bearing;

import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import dev.simulated_team.simulated.fabric.block.NativeEntityBlock;
import dev.simulated_team.simulated.api.CustomStressImpactTooltipProvider;
import dev.simulated_team.simulated.index.SimBlockMovementChecks;
import dev.ryanhcode.offroad.data.OffroadLang;
import dev.ryanhcode.offroad.index.OffroadBlockEntityTypes;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class BoreheadBearingBlock extends DirectionalAxisKineticBlock implements dev.simulated_team.simulated.fabric.block.NativeWrenchable, NativeEntityBlock<BoreheadBearingBlockEntity>, CustomStressImpactTooltipProvider {

    private static final ObjectList<BlockPos> TEMP_POSITIONS = new ObjectArrayList<>();

    static {
        
        SimBlockMovementChecks.registerAdditionalBlocks( (blockState, level, blockPos, set) -> {
            TEMP_POSITIONS.clear();

            if (blockState.getBlock() instanceof BoreheadBearingBlock) {
                TEMP_POSITIONS.addFirst(blockPos.relative(blockState.getValue(FACING)));
            }

            return TEMP_POSITIONS;
        });

    }


    public BoreheadBearingBlock(final Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useItemOn(final ItemStack stack, final BlockState state, final Level level, final BlockPos pos, final Player player, final InteractionHand hand, final BlockHitResult hitResult) {
        if (!player.mayBuild())
            return InteractionResult.FAIL;
        if (player.isShiftKeyDown())
            return InteractionResult.FAIL;
        if (stack.isEmpty()) {
            if (level.isClientSide())
                return InteractionResult.SUCCESS;
            this.withBlockEntityDo(level, pos, be -> {
                if (be.isRunning()) {
                    be.startDisassemblySlowdown();
                    return;
                }

                be.setAssembleNextTick(true);
            });
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    public boolean hasShaftTowards(final LevelReader world, final BlockPos pos, final BlockState state, final Direction face) {
        return super.hasShaftTowards(world, pos, state, face);
    }

    @Override
    public Class<BoreheadBearingBlockEntity> getBlockEntityClass() {
        return BoreheadBearingBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BoreheadBearingBlockEntity> getBlockEntityType() {
        return OffroadBlockEntityTypes.BOREHEAD_BEARING;
    }

    @Override
    public InteractionResult onWrenched(final BlockState state, final UseOnContext context) {
        final InteractionResult resultType = dev.simulated_team.simulated.fabric.block.NativeWrenchable.super.onWrenched(state, context);
        if (!context.getLevel().isClientSide() && resultType.consumesAction()) {
            final BlockEntity be = context.getLevel().getBlockEntity(context.getClickedPos());

            if (be instanceof BoreheadBearingBlockEntity) {
                if (context.getLevel().getBlockState(context.getClickedPos()).getValue(FACING) != state.getValue(FACING)) {
                    ((BoreheadBearingBlockEntity) be).disassemble();
                }
            }
        }

        return resultType;
    }

    @Override
    public LangBuilder getCustomImpactLang() {
        return OffroadLang.translate("tooltip.borehead_bearing_stress");
    }

    @Override
    public int getBarLength() {
        return 4;
    }

    @Override
    public int getFilledBarLength() {
        return 4;
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