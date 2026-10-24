package dev.ryanhcode.offroad.content.blocks.rock_cutting_wheel;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import dev.simulated_team.simulated.fabric.block.NativeEntityBlock;
import com.simibubi.create.impl.contraption.BlockMovementChecksImpl;
import dev.ryanhcode.offroad.index.OffroadBlockEntityTypes;
import dev.simulated_team.simulated.content.blocks.util.AbstractDirectionalAxisBlock;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class RockCuttingWheelBlock extends AbstractDirectionalAxisBlock implements NativeEntityBlock<RockCuttingWheelBlockEntity> {


    static {
        BlockMovementChecksImpl.registerAttachedCheck((state, world, pos, direction) -> {
            if (state.getBlock() instanceof RockCuttingWheelBlock) {
                if (direction != state.getValue(BlockStateProperties.FACING)) {
                    return BlockMovementChecks.CheckResult.SUCCESS;
                } else {
                    return BlockMovementChecks.CheckResult.FAIL;
                }
            }

            return BlockMovementChecks.CheckResult.PASS;
        });

        BlockMovementChecksImpl.registerNotSupportiveCheck(((state, direction) -> {
            if (state.getBlock() instanceof RockCuttingWheelBlock && direction.equals(state.getValue(BlockStateProperties.FACING))) {
                return BlockMovementChecks.CheckResult.FAIL;
            }

            return BlockMovementChecks.CheckResult.PASS;
        }));

    }

    public RockCuttingWheelBlock(final Properties properties) {
        super(properties);
    }

    @Override
    public Class<RockCuttingWheelBlockEntity> getBlockEntityClass() {
        return RockCuttingWheelBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends RockCuttingWheelBlockEntity> getBlockEntityType() {
        return OffroadBlockEntityTypes.ROCKCUTTING_WHEEL_BLOCK_ENTITY;
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
