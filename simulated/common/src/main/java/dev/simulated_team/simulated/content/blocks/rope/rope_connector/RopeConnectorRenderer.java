package dev.simulated_team.simulated.content.blocks.rope.rope_connector;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.RopeStrandRenderer;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import dev.simulated_team.simulated.fabric.registry.NativePartTransforms;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public final class RopeConnectorRenderer extends NativePartsRenderer<RopeConnectorBlockEntity> {
    public RopeConnectorRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(RopeConnectorBlockEntity entity, Vec3 camera) { return true; }
    @Override protected void extractParts(RopeConnectorBlockEntity entity, float partial, Parts parts) {
        final var holder = entity.getRopeHolder();
        RopeStrandRenderer.extract(entity, holder, partial, parts);
        if (!holder.isAttached() && (!entity.isVirtual() || !holder.renderAttached)) return;
        final var state = entity.getBlockState();
        final var pos = entity.getBlockPos();
        final var attachment = entity.getVisualAttachmentPoint(pos, state).subtract(Vec3.atCenterOf(pos));
        final var facing = state.getValue(RopeConnectorBlock.FACING);
        final boolean first = state.getValue(RopeConnectorBlock.AXIS_ALONG_FIRST_COORDINATE);
        final var pose = new PoseStack();
        pose.translate(attachment.x, attachment.y, attachment.z);
        rotateCentered(pose, Direction.Axis.Z, facing == Direction.UP ? 270 : facing == Direction.DOWN ? 90 : 0);
        rotateCentered(pose, Direction.Axis.Y, AngleHelper.horizontalAngle(facing) + (first || facing.getAxis() != Direction.Axis.Y ? 0 : 90));
        rotateCentered(pose, Direction.Axis.Z, (first ^ facing.getAxis() == Direction.Axis.Z) ? 90 : 0);
        rotateCentered(pose, Direction.Axis.Y, 90);
        NativePartTransforms.partialFacing(pose, Direction.NORTH);
        parts.add(SimPartialModels.ROPE_CONNECTOR_KNOT, pose);
    }
}
