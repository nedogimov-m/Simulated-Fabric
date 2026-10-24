package dev.simulated_team.simulated.content.blocks.auger_shaft;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import dev.simulated_team.simulated.fabric.registry.NativePartTransforms;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

public final class AugerShaftRenderer extends NativePartsRenderer<AugerShaftBlockEntity> {
    public AugerShaftRenderer(BlockEntityRendererProvider.Context context) {}
    @Override protected void extractParts(AugerShaftBlockEntity entity, float partial, Parts parts) {
        final var axis = rotationAxis(entity);
        final var facing = Direction.get(Direction.AxisDirection.POSITIVE, axis);
        parts.add(AllBlocks.SHAFT.defaultBlockState().setValue(ShaftBlock.AXIS, axis), rotating(entity, partial, axis));
        if (!(entity.getBlockState().getBlock() instanceof AugerCogBlock)) return;
        final var cog = rotating(entity, partial, axis);
        NativePartTransforms.faceNorth(cog, facing);
        rotateCentered(cog, Direction.Axis.X, -90);
        parts.add(SimPartialModels.AUGER_COG, cog);
        for (int side = 0; side < 2; side++) {
            final var pose = new PoseStack();
            NativePartTransforms.faceNorth(pose, facing);
            rotateCentered(pose, Direction.Axis.X, -(facing.getAxis().isHorizontal() ? 90 : 0) - side * 180);
            // Source partialFacing was baked into the mesh before the separate instance transform.
            NativePartTransforms.partialFacing(pose, facing);
            final boolean powered = entity.getSpeed() != 0 && entity.flowDirection == (side == 1 ? facing.getOpposite() : facing);
            parts.add(powered ? SimPartialModels.AUGER_REDSTONE_ON : SimPartialModels.AUGER_REDSTONE_OFF, pose);
        }
    }
}
