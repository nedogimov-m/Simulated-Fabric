package dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import dev.eriksonn.aeronautics.index.AeroPartialModels;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class PropellerBearingRenderer extends NativePartsRenderer<PropellerBearingBlockEntity> {
    public PropellerBearingRenderer(BlockEntityRendererProvider.Context context) {}
    @Override protected void extractParts(PropellerBearingBlockEntity entity, float partial, Parts parts) {
        final Direction facing = entity.getBlockState().getValue(BlockStateProperties.FACING);
        final var shaft = rotating(entity, partial, rotationAxis(entity));
        faceSouth(shaft, facing.getOpposite());
        parts.add(AllPartialModels.SHAFT_HALF, shaft);
        final var plate = new PoseStack();
        rotateCentered(plate, facing.getAxis(), entity.getInterpolatedAngle(partial - 1));
        if (facing.getAxis().isHorizontal()) rotateCentered(plate, Direction.Axis.Y, AngleHelper.horizontalAngle(facing.getOpposite()));
        rotateCentered(plate, Direction.Axis.X, -90 - AngleHelper.verticalAngle(facing));
        parts.add(AeroPartialModels.BEARING_PLATE, plate);
    }
}
