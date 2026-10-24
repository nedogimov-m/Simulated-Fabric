package dev.eriksonn.aeronautics.content.blocks.propeller.small;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.createmod.catnip.math.AngleHelper;

public abstract class SimplePropellerRenderer<T extends BasePropellerBlockEntity> extends NativePartsRenderer<T> {
    public SimplePropellerRenderer(BlockEntityRendererProvider.Context context) {}

    @Override protected void extractParts(T entity, float partialTick, Parts parts) {
        final Direction facing = entity.getBlockState().getValue(BlockStateProperties.FACING);
        final var shaft = rotating(entity, partialTick, rotationAxis(entity));
        faceSouth(shaft, facing.getOpposite());
        parts.add(AllPartialModels.SHAFT_HALF, shaft);
        final var propeller = new PoseStack();
        rotateCentered(propeller, facing.getAxis(), (float) Math.toDegrees(getAngle(partialTick, facing, entity)));
        if (facing.getAxis().isHorizontal()) rotateCentered(propeller, Direction.Axis.Y, AngleHelper.horizontalAngle(facing.getOpposite()));
        if (facing.getAxis().isVertical()) rotateCentered(propeller, Direction.Axis.X, AngleHelper.verticalAngle(facing.getOpposite()));
        propeller.translate(0, 0, -3 / 16f);
        rotateCentered(propeller, Direction.Axis.X, -90 - AngleHelper.verticalAngle(facing));
        faceSouth(propeller, facing);
        parts.add(getCurrentModel(entity), propeller);
    }
    public abstract ExtraModelKey<BlockStateModel> getCurrentModel(T entity);
    public float getAngle(float partialTick, Direction facing, T entity) {
        return (float) Math.toRadians(entity.getPreviousAngle() * (1 - partialTick) + entity.getAngle() * partialTick) * 2;
    }
}
