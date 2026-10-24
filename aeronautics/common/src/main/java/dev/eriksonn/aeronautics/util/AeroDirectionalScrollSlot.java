package dev.eriksonn.aeronautics.util;

import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import java.util.function.BiPredicate;

/** Create's original directional extender slot geometry on the native matrix API. */
public class AeroDirectionalScrollSlot extends CenteredSideValueBoxTransform {
    public AeroDirectionalScrollSlot(BiPredicate<BlockState, Direction> allowedDirections) { super(allowedDirections); }
    @Override public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
        return super.getLocalOffset(level, pos, state).add(Vec3.atLowerCornerOf(
                state.getValue(BlockStateProperties.FACING).getUnitVec3i()).scale(-2 / 16f));
    }
    @Override public void rotate(LevelAccessor level, BlockPos pos, BlockState state, Matrix4f matrix) {
        if (!getSide().getAxis().isHorizontal()) matrix.rotateY(AngleHelper.rad(
                AngleHelper.horizontalAngle(state.getValue(BlockStateProperties.FACING)) + 180));
        super.rotate(level, pos, state, matrix);
    }
}
