package dev.simulated_team.simulated.content.blocks.redstone.modulating_receiver;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class ModulatingLinkedReceiverRenderer extends NativePartsRenderer<ModulatingLinkedReceiverBlockEntity> {
    public static final float MAX_DISTANCE = 256, SMOOTHING = 20;
    public ModulatingLinkedReceiverRenderer(BlockEntityRendererProvider.Context context) {}
    @Override protected void extractParts(ModulatingLinkedReceiverBlockEntity entity, float partial, Parts parts) {
        final var facing = entity.getBlockState().getValue(BlockStateProperties.FACING);
        for (int i = 0; i < 2; i++) {
            final float range = i == 0 ? entity.minRange : entity.maxRange;
            final float offset = 5.5f * ((range - 1) * (SMOOTHING + MAX_DISTANCE - 1))
                    / ((MAX_DISTANCE - 1) * (SMOOTHING + range - 1)) + (i == 0 ? 0 : .5f);
            final var pose = new PoseStack();
            pose.translate(facing.getStepX() * offset / 16, facing.getStepY() * offset / 16, facing.getStepZ() * offset / 16);
            if (facing.getAxis().isHorizontal())
                rotateCentered(pose, Direction.Axis.Y, AngleHelper.horizontalAngle(facing.getOpposite()));
            rotateCentered(pose, Direction.Axis.X, -90 - AngleHelper.verticalAngle(facing));
            parts.add(SimPartialModels.MODULATING_RECEIVER_PLATE, pose);
        }
    }
}
