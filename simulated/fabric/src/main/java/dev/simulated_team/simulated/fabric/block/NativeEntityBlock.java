package dev.simulated_team.simulated.fabric.block;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/** Actual native entity ownership, typed lookup and Create smart lifecycle for addon blocks. */
public interface NativeEntityBlock<T extends BlockEntity> extends EntityBlock {
    Class<T> getBlockEntityClass();
    BlockEntityType<? extends T> getBlockEntityType();

    default T getBlockEntity(BlockGetter level, BlockPos pos) {
        final BlockEntity entity = level.getBlockEntity(pos);
        return this.getBlockEntityClass().isInstance(entity) ? this.getBlockEntityClass().cast(entity) : null;
    }
    default Optional<T> getBlockEntityOptional(BlockGetter level, BlockPos pos) { return Optional.ofNullable(this.getBlockEntity(level, pos)); }
    default void withBlockEntityDo(BlockGetter level, BlockPos pos, Consumer<T> action) { this.getBlockEntityOptional(level, pos).ifPresent(action); }
    default InteractionResult onBlockEntityUse(BlockGetter level, BlockPos pos, Function<T, InteractionResult> action) {
        return this.getBlockEntityOptional(level, pos).map(action).orElse(InteractionResult.PASS);
    }
    default InteractionResult onBlockEntityUseItemOn(BlockGetter level, BlockPos pos, Function<T, InteractionResult> action) {
        return this.getBlockEntityOptional(level, pos).map(action).orElse(InteractionResult.TRY_WITH_EMPTY_HAND);
    }
    @Override default BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return create(this, pos, state); }
    @Override default <S extends BlockEntity> BlockEntityTicker<S> getTicker(Level level, BlockState state, BlockEntityType<S> type) {
        return ticker(this, level, state, type);
    }
    // Explicit class overrides delegate here: inherited concrete Create factories take precedence over interface defaults.
    static BlockEntity create(NativeEntityBlock<?> owner, BlockPos pos, BlockState state) {
        final BlockEntityType<?> type = owner.getBlockEntityType();
        return type == null ? null : type.create(pos, state);
    }
    static <S extends BlockEntity> BlockEntityTicker<S> ticker(NativeEntityBlock<?> owner, Level level, BlockState state, BlockEntityType<S> type) {
        return type == owner.getBlockEntityType() && SmartBlockEntity.class.isAssignableFrom(owner.getBlockEntityClass())
                ? new SmartBlockEntityTicker<>() : null;
    }
    static void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState) {
        if (!oldState.hasBlockEntity() || oldState.is(newState.getBlock()) && newState.hasBlockEntity()) return;
        if (level.getBlockEntity(pos) instanceof SmartBlockEntity smart) smart.destroy();
        level.removeBlockEntity(pos);
    }
}
