package dev.eriksonn.aeronautics.content.blocks.mounted_potato_cannon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import dev.eriksonn.aeronautics.index.AeroPartialModels;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Quaternionf;

/** Extracts all five moving components and the in-flight ammunition before the submission phase. */
public class MountedPotatoCannonRenderer extends NativePartsRenderer<MountedPotatoCannonBlockEntity> {
    private final ItemModelResolver items;
    public MountedPotatoCannonRenderer(BlockEntityRendererProvider.Context context) { this.items = context.itemModelResolver(); }
    @Override protected void extractParts(MountedPotatoCannonBlockEntity entity, float partial, Parts parts) {
        final var state = entity.getBlockState();
        final var axis = rotationAxis(entity);
        parts.add(AllBlocks.SHAFT.defaultBlockState().setValue(ShaftBlock.AXIS, axis), rotating(entity, partial, axis));
        final float barrelOffset = entity.isBlocked() ? (float) -(entity.getBlockedLength() / 2) : entity.getBarrelDistance(partial);
        final float bellowOffset = -entity.getBellowDistance(partial);
        final var barrel = transform(state);
        barrel.translate(0, 0, barrelOffset);
        parts.add(AeroPartialModels.CANNON_BARREL, barrel);
        final var bellow = transform(state);
        bellow.translate(0, bellowOffset, 0);
        parts.add(AeroPartialModels.CANNON_BELLOW, bellow);
        // The original SuperByteBuffer resets after renderInto, so the second bellow starts afresh.
        final var secondBellow = transform(state);
        rotateCentered(secondBellow, Direction.Axis.Z, 180);
        secondBellow.translate(0, bellowOffset, 0);
        parts.add(AeroPartialModels.CANNON_BELLOW, secondBellow);
        final var cog = transform(state);
        rotateCentered(cog, Direction.Axis.Z, entity.getCogwheelAngle(partial) % 360);
        parts.add(AeroPartialModels.CANNON_COG, cog);
        if (entity.getInventory().isEmpty()) return;
        final var itemPose = new PoseStack();
        itemPose.translate(.5, .5, .5);
        final var facing = state.getValue(BlockStateProperties.FACING);
        final float position = entity.isBlocked() ? 0 : .8f * (1 - (float) Math.exp(-.25f * entity.getItemTime(partial)));
        itemPose.translate(facing.getStepX() * position, facing.getStepY() * position, facing.getStepZ() * position);
        itemPose.scale(.35f, .35f, .35f);
        final int rotation = entity.getItemRotationId();
        itemPose.rotate(new Quaternionf((float) Math.sin(rotation * .4f), (float) Math.cos(rotation * 1.4f),
                (float) Math.sin(rotation * 3f), (float) Math.cos(rotation * 5f)).normalize());
        final var item = new ItemStackRenderState();
        this.items.updateForTopItem(item, entity.getInventory().slot.getStack(), ItemDisplayContext.FIXED, entity.getLevel(), null, 0);
        parts.addItem(item, itemPose);
    }
    private static PoseStack transform(BlockState state) {
        final Direction facing = state.getValue(BlockStateProperties.FACING);
        final float last = state.getValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE) ^ facing.getAxis() == Direction.Axis.Z ? 90 : 0;
        final float vertical = facing.getAxis().isVertical() ? 90 : 0;
        final var pose = new PoseStack();
        rotateCentered(pose, Direction.Axis.Z, vertical);
        rotateCentered(pose, Direction.Axis.Y, -vertical);
        rotateCentered(pose, Direction.Axis.Y, AngleHelper.horizontalAngle(facing));
        rotateCentered(pose, Direction.Axis.Z, last);
        rotateCentered(pose, Direction.Axis.Y, facing == Direction.UP ? 180 : 0);
        return pose;
    }
}
