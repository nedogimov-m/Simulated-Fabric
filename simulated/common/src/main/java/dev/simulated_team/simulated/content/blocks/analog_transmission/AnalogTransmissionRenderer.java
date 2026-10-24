package dev.simulated_team.simulated.content.blocks.analog_transmission;

import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

/** Both independent kinetic owners are sampled during extraction. */
public final class AnalogTransmissionRenderer extends NativePartsRenderer<AnalogTransmissionBlockEntity> {
    public AnalogTransmissionRenderer(BlockEntityRendererProvider.Context context) {}
    @Override protected void extractParts(AnalogTransmissionBlockEntity entity, float partial, Parts parts) {
        final Direction.Axis axis = rotationAxis(entity);
        final var cog = rotating(entity.getExtraKinetics(), partial, axis);
        final Direction facing = Direction.get(Direction.AxisDirection.POSITIVE, axis);
        cog.translate(.5, .5, .5);
        cog.rotate(Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(facing)));
        cog.rotate(Axis.XP.rotationDegrees(AngleHelper.verticalAngle(facing) + 90));
        cog.translate(-.5, -.5, -.5);
        parts.add(SimPartialModels.ANALOG_TRANSMISSION_COG, cog);
        parts.add(AllBlocks.SHAFT.defaultBlockState().setValue(ShaftBlock.AXIS, axis), rotating(entity, partial, axis));
    }
}
