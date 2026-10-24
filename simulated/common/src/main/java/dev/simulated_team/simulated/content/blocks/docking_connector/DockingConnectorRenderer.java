package dev.simulated_team.simulated.content.blocks.docking_connector;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Matrix4f;
import org.joml.Vector2f;

/** Source piston/foot linkage extracted into fourteen independent native parts. */
public final class DockingConnectorRenderer extends NativePartsRenderer<DockingConnectorBlockEntity> {
    public DockingConnectorRenderer(BlockEntityRendererProvider.Context context) {}
    @Override protected void extractParts(DockingConnectorBlockEntity entity, float partial, Parts parts) {
        final float extension = entity.getExtensionDistance(partial);
        final float rotation = entity.getFeetRotation(partial) * 90;
        final var base = new PoseStack();
        rotateToFaceCentered(base, entity.getBlockState().getValue(BlockStateProperties.FACING));
        base.pushPose(); base.translate(0, extension * .5, 0);
        parts.add(SimPartialModels.DOCKING_CONNECTOR_MAIN_PISTON_BOTTOM, base); base.popPose();
        base.pushPose(); base.translate(0, extension, 0);
        parts.add(SimPartialModels.DOCKING_CONNECTOR_MAIN_PISTON_TOP, base); base.popPose();
        final var foot = new Vector2f(-7.5f, 15.5f).div(16).add(0, extension);
        final var top = rotate(new Vector2f(1.5f, -2.5f).div(16), rotation).add(foot);
        final var bottom = new Vector2f(-6, 2).div(16).add(0, extension / 2);
        final var relative = new Vector2f(top).sub(bottom).normalize();
        final var linkage = new Matrix4f(1, 0, 0, 0, 0, relative.y, relative.x, 0,
                0, -relative.x, relative.y, 0, 0, 0, 0, 1);
        for (int side = 0; side < 4; side++) {
            base.pushPose(); base.translate(.5, 0, .5);
            base.rotate(com.mojang.math.Axis.YP.rotationDegrees(side * 90));
            base.pushPose(); base.translate(0, bottom.y, bottom.x); base.mulPose(linkage);
            parts.add(SimPartialModels.DOCKING_CONNECTOR_SIDE_PISTON_BOTTOM, base); base.popPose();
            base.pushPose(); base.translate(0, top.y, top.x); base.mulPose(linkage);
            parts.add(SimPartialModels.DOCKING_CONNECTOR_SIDE_PISTON_TOP, base); base.popPose();
            base.pushPose(); base.translate(0, foot.y, foot.x);
            base.rotate(com.mojang.math.Axis.XP.rotationDegrees(rotation));
            parts.add(SimPartialModels.DOCKING_CONNECTOR_FOOT, base); base.popPose();
            base.popPose();
        }
    }
    @Override public int getViewDistance() { return 256; }
    public static void rotateToFaceCentered(PoseStack pose, Direction facing) {
        rotateCentered(pose, Direction.Axis.Y, AngleHelper.horizontalAngle(facing));
        rotateCentered(pose, Direction.Axis.X, AngleHelper.verticalAngle(facing) + 90);
    }
    private static Vector2f rotate(Vector2f vector, float degrees) {
        final float angle = (float) Math.toRadians(degrees), s = Mth.sin(angle), c = Mth.cos(angle);
        return vector.set(vector.x * c + vector.y * s, vector.y * c - vector.x * s);
    }
}
