package dev.eriksonn.aeronautics.content.blocks.propeller.small.smart_propeller;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.SimplePropellerRenderer;
import dev.eriksonn.aeronautics.index.AeroPartialModels;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.createmod.catnip.math.AngleHelper;

import static dev.eriksonn.aeronautics.content.blocks.propeller.small.smart_propeller.SmartPropellerBlock.REVERSED;

public class SmartPropellerRenderer extends SimplePropellerRenderer<SmartPropellerBlockEntity> {
    public SmartPropellerRenderer(BlockEntityRendererProvider.Context context) { super(context); }
    @Override protected void extractParts(SmartPropellerBlockEntity entity, float partial, Parts parts) {
        final var shaft = rotating(entity, partial, rotationAxis(entity));
        faceSouth(shaft, Direction.DOWN);
        parts.add(AllPartialModels.SHAFT_HALF, shaft);
        final Direction horizontal = Direction.fromAxisAndDirection(
                entity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_AXIS), Direction.AxisDirection.NEGATIVE);
        final var propeller = new PoseStack();
        final var hinge = new PoseStack();
        final float tilt = entity.getLerpedHingeAngle(partial);
        rotateCentered(propeller, horizontal.getClockWise().getAxis(), tilt * horizontal.getClockWise().getAxisDirection().getStep());
        rotateCentered(hinge, horizontal.getClockWise().getAxis(), tilt * horizontal.getClockWise().getAxisDirection().getStep());
        rotateCentered(propeller, Direction.Axis.Y, AngleHelper.horizontalAngle(horizontal));
        rotateCentered(hinge, Direction.Axis.Y, AngleHelper.horizontalAngle(horizontal));
        rotateCentered(propeller, Direction.Axis.Y, (float) Math.toDegrees(getAngle(partial, Direction.UP, entity)));
        propeller.translate(0, 10 / 16f, 0);
        rotateCentered(propeller, Direction.Axis.X, 90);
        hinge.translate(0, -1 / 16f, 0);
        rotateCentered(hinge, Direction.Axis.X, 90);
        faceSouth(propeller, Direction.UP);
        faceSouth(hinge, Direction.UP);
        parts.add(getCurrentModel(entity), propeller);
        parts.add(AeroPartialModels.SMART_PROPELLER_HINGE, hinge);
    }
    @Override public ExtraModelKey<BlockStateModel> getCurrentModel(SmartPropellerBlockEntity entity) {
        return entity.getBlockState().getValue(REVERSED) ? AeroPartialModels.SMART_PROPELLER_REVERSED : AeroPartialModels.SMART_PROPELLER;
    }
}
