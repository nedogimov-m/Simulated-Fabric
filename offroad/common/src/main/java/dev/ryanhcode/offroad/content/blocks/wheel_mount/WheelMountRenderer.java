package dev.ryanhcode.offroad.content.blocks.wheel_mount;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import dev.ryanhcode.offroad.fabric.registry.NativeTireModels;
import dev.ryanhcode.offroad.index.OffroadDataComponents;
import dev.ryanhcode.offroad.index.OffroadPartialModels;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import dev.simulated_team.simulated.util.SimColors;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Extracts both suspension links, the animated tire, springs, indicators and input shaft. */
public class WheelMountRenderer extends NativePartsRenderer<WheelMountBlockEntity> {
    private final ItemModelResolver items;
    public WheelMountRenderer(BlockEntityRendererProvider.Context context) { this.items = context.itemModelResolver(); }
    @Override protected void extractParts(WheelMountBlockEntity entity, float partial, Parts parts) {
        final var state = entity.getBlockState();
        final var shaft = rotating(entity, partial, rotationAxis(entity));
        faceSouth(shaft, state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite());
        parts.add(AllPartialModels.SHAFT_HALF, shaft);
        final Direction direction = state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
        final double horizontalWheel = 22d / 16;
        final double verticalWheel = -entity.getLerpedExtension(partial);
        final double telescopeX = horizontalWheel - 10d / 16;
        final double telescopeY = verticalWheel + 6d / 16;
        final double telescopeAngle = Math.atan2(telescopeY, telescopeX);
        final double telescopeDistance = Math.hypot(telescopeY, telescopeX);
        final double springX = horizontalWheel - 12d / 16 - 7d / 16;
        final double springY = verticalWheel + 2d / 16 - 7d / 16;
        final double springAngle = Math.atan2(springY, springX);
        final double springDistance = Math.hypot(springY, springX);
        final var pose = new PoseStack();
        rotateCentered(pose, Direction.Axis.Y, AngleHelper.horizontalAngle(direction));
        rotateCentered(pose, Direction.Axis.X, AngleHelper.verticalAngle(direction));
        pose.pushPose();
        pose.translate(0, -6d / 16, 0);
        rotateCentered(pose, Direction.Axis.X, (float) Math.toDegrees(telescopeAngle));
        parts.add(OffroadPartialModels.TELE_OUTER, pose);
        pose.translate(0, 0, -(telescopeDistance - 1));
        parts.add(OffroadPartialModels.TELE_INNER, pose);
        pose.popPose();
        pose.pushPose();
        pose.translate(0, verticalWheel, 26d / 16 - horizontalWheel);
        pose.translate(.5, .5, .5);
        pose.rotateAround(Axis.YP.rotation((float) entity.getLerpedYaw(partial)), 0, 0, (float) (-horizontalWheel + 6d / 16));
        pose.translate(-.5, -.5, -.5);
        parts.add(OffroadPartialModels.TELE_MOUNT, pose);
        pose.translate(.5, .5, .5);
        pose.translate(0, 0, -26d / 16);
        final double spin = -entity.getLerpedAngle(partial)
                * (direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1)
                * (direction.getAxis() == Direction.Axis.X ? 1 : -1);
        pose.rotate(Axis.ZP.rotation((float) spin));
        final var stack = entity.getHeldItem();
        final var tire = stack.get(OffroadDataComponents.TIRE);
        if (tire != null) {
            pose.rotateDegrees(Axis.XP, (float) tire.rotation().x);
            pose.rotateDegrees(Axis.YP, (float) tire.rotation().y);
            pose.rotateDegrees(Axis.ZP, (float) tire.rotation().z);
            pose.translate(tire.offset());
            if (tire.model().isPresent()) {
                pose.translate(-.5, 0, -.5);
                parts.add(NativeTireModels.extract(tire.model().get()), pose);
            } else {
                final var item = new ItemStackRenderState();
                this.items.updateForTopItem(item, stack, ItemDisplayContext.NONE, entity.getLevel(), null, 0);
                parts.addItem(item, pose);
            }
        }
        pose.popPose();
        pose.pushPose();
        final double springMountY = 7d / 16;
        final double springMountZ = 7d / 16;
        pose.translate(.5, .5 + springMountY, .5 - springMountZ);
        pose.rotate(Axis.XP.rotation((float) springAngle + Mth.PI / 2));
        pose.translate(-.5, -.5 - springMountY, -.5 + springMountZ);
        final float span = (float) springDistance - 4f / 16;
        parts.add(OffroadPartialModels.SPRING_UPPER, pose);
        pose.pushPose();
        pose.translate(0, 13d / 16, 0);
        pose.scale(1, span / (14f / 16), 1);
        pose.translate(0, -13d / 16, 0);
        parts.add(OffroadPartialModels.SPRING_MIDDLE, pose);
        pose.popPose();
        pose.translate(0, -(span - 14d / 16), 0);
        parts.add(OffroadPartialModels.SPRING_LOWER, pose);
        pose.popPose();
        parts.add(OffroadPartialModels.DIODE_LEFT, pose, SimColors.redstone(entity.clientSteeringSignalLeft / 15f),
                ChunkSectionLayer.CUTOUT, false, parts.light());
        parts.add(OffroadPartialModels.DIODE_RIGHT, pose, SimColors.redstone(entity.clientSteeringSignalRight / 15f),
                ChunkSectionLayer.CUTOUT, false, parts.light());
    }
    @Override public int getViewDistance() { return 512; }
}
