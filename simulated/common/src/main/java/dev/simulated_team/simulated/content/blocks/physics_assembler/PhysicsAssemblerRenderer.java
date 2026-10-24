package dev.simulated_team.simulated.content.blocks.physics_assembler;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.redstone.analogLever.AnalogLeverBlock;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.properties.AttachFace;

public final class PhysicsAssemblerRenderer extends NativePartsRenderer<PhysicsAssemblerBlockEntity> {
    public PhysicsAssemblerRenderer(BlockEntityRendererProvider.Context context) {}

    @Override protected void extractParts(PhysicsAssemblerBlockEntity entity, float partial, Parts parts) {
        final var state = entity.getBlockState();
        final var face = state.getValue(AnalogLeverBlock.FACE);
        final float x = face == AttachFace.FLOOR ? 0 : face == AttachFace.WALL ? 90 : 180;
        final var pose = new PoseStack();
        pose.translate(.5, .5, .5);
        pose.rotate(Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(state.getValue(AnalogLeverBlock.FACING))));
        pose.rotate(Axis.XP.rotationDegrees(x));
        pose.translate(-.5, -.5, -.5);
        pose.translate(.5, 7 / 16f, .5);
        pose.rotate(Axis.XP.rotation(getRenderAngle(entity, partial)));
        pose.translate(-.5, -7 / 16f, -.5);
        parts.add(SimPartialModels.ASSEMBLER_LEVER, pose);
    }

    public static float getRenderAngle(PhysicsAssemblerBlockEntity entity, float partial) {
        if (!entity.isVirtual()) entity.initializeLeverPosition();
        return (float) Math.toRadians(entity.getClientAngle(partial));
    }
}
