package dev.eriksonn.aeronautics.fabric.fluid;

import dev.eriksonn.aeronautics.api.levitite_blend_crystallization.LevititeBlendDummyInterface;
import dev.eriksonn.aeronautics.index.AeroSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.*;
import java.util.Optional;

/** Native source/flowing pair with the original levitite spread parameters. */
public abstract class LevititeBlendFluid extends FlowingFluid {
    @Override public Fluid getSource() { return NativeAeroFluids.SOURCE; }
    @Override public Fluid getFlowing() { return NativeAeroFluids.FLOWING; }
    @Override public Item getBucket() { return NativeAeroFluids.BUCKET; }
    @Override public boolean isSame(Fluid other) { return other == getSource() || other == getFlowing(); }
    @Override protected boolean canConvertToSource(ServerLevel level) { return false; }
    @Override public int getSlopeFindDistance(LevelReader level) { return 3; }
    @Override public int getDropOff(LevelReader level) { return 2; }
    @Override public int getTickDelay(LevelReader level) { return 25; }
    @Override public float getExplosionResistance() { return 100; }
    @Override public boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid other, Direction side) { return false; }
    @Override public BlockState createLegacyBlock(FluidState state) {
        return NativeAeroFluids.BLOCK.defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
    }
    @Override protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
        Block.dropResources(state, level, pos, state.hasBlockEntity() ? level.getBlockEntity(pos) : null);
    }
    @Override public Optional<SoundEvent> getPickupSound() { return Optional.of(AeroSoundEvents.LEVITITE_BLEND_FILL.event()); }

    public static final class Source extends LevititeBlendFluid implements LevititeBlendDummyInterface {
        @Override public boolean isSource(FluidState state) { return true; }
        @Override public int getAmount(FluidState state) { return 8; }
        @Override public void tick(ServerLevel level, BlockPos pos, BlockState block, FluidState state) {
            super.tick(level, pos, block, state);
            // Only the original custom source fluid starts/propagates crystallization.
            levititeBlendTick(level, pos, state);
        }
    }
    public static final class Flowing extends LevititeBlendFluid {
        public Flowing() { registerDefaultState(defaultFluidState().setValue(LEVEL, 7)); }
        @Override protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder); builder.add(LEVEL);
        }
        @Override public boolean isSource(FluidState state) { return false; }
        @Override public int getAmount(FluidState state) { return state.getValue(LEVEL); }
    }
}
