package dev.eriksonn.aeronautics.fabric.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;

public final class LevititeFluidPhysics {
    public static final TagKey<Fluid> BLEND = TagKey.create(Registries.FLUID, Identifier.parse("aeronautics:levitite_blend"));
    public static final int DENSITY = 1400;
    public static final double CURRENT_SCALE = -7 / 3000f;

    /** Original lava-style drag, with gravity reduced by immersion unless crouching. */
    public static void travel(LivingEntity entity, Vec3 input, double gravity) {
        final double originalY = entity.getY();
        final boolean falling = entity.getDeltaMovement().y < 0;
        final double height = entity.getFluidHeight(BLEND);
        if (!entity.isCrouching()) gravity = Math.clamp(gravity * (1 - height), 0, gravity);
        entity.moveRelative(0.02F, input);
        entity.move(MoverType.SELF, entity.getDeltaMovement());
        if (height <= entity.getFluidJumpThreshold()) {
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.5, 0.7F, 0.5));
            entity.setDeltaMovement(entity.getFluidFallingAdjustedMovement(gravity, falling, entity.getDeltaMovement()));
        } else entity.setDeltaMovement(entity.getDeltaMovement().scale(0.5));
        if (gravity != 0) entity.setDeltaMovement(entity.getDeltaMovement().add(0, -gravity / 4, 0));
        final Vec3 velocity = entity.getDeltaMovement();
        if (entity.horizontalCollision && entity.isFree(velocity.x, velocity.y + 0.6F - entity.getY() + originalY, velocity.z))
            entity.setDeltaMovement(velocity.x, 0.3F, velocity.z);
    }

    public static void reactLava(Level level, BlockPos pos) {
        if (level.isClientSide() || !level.isLoaded(pos)) return;
        final var lava = level.getFluidState(pos);
        if (!lava.is(FluidTags.LAVA)) return;
        for (final var side : Direction.values()) {
            if (side == Direction.DOWN || !level.isLoaded(pos.relative(side))) continue;
            if (!level.getFluidState(pos.relative(side)).getType().isSame(NativeAeroFluids.SOURCE)) continue;
            level.setBlockAndUpdate(pos, (lava.isSource() ? Blocks.OBSIDIAN : Blocks.CALCITE).defaultBlockState());
            level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 2.6f);
            return;
        }
    }
    private LevititeFluidPhysics() {}
}
