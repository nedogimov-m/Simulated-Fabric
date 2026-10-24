package dev.simulated_team.simulated.content.blocks.absorber;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.simulated_team.simulated.fabric.registry.NativeAbsorberAnimation;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.joml.Matrix4fc;

/** Wet/dry sponge, hat, pivot and mirrored source linkage through native extracted partials. */
public final class AbsorberRenderer extends NativePartsRenderer<AbsorberBlockEntity> {
    public AbsorberRenderer(BlockEntityRendererProvider.Context context) { }
    @Override protected void extractParts(AbsorberBlockEntity entity,float partial,Parts parts) {
        final var block=entity.getBlockState();
        final var frame=NativeAbsorberAnimation.extract(entity.animationTimer.getValue(partial),entity.animationTimer.getChaseTarget(),
                (float)Math.toRadians(AngleHelper.horizontalAngle(block.getValue(AbsorberBlock.HORIZONTAL_FACING))+180));
        parts.add(block.getValue(AbsorberBlock.WET)?SimPartialModels.ABSORBER_SPONGE_WET:SimPartialModels.ABSORBER_SPONGE_DRY,pose(frame.sponge()));
        parts.add(SimPartialModels.ABSORBER_HAT,pose(frame.hat()));parts.add(SimPartialModels.ABSORBER_PIVOT,pose(frame.pivot()));
        parts.add(SimPartialModels.ABSORBER_ARM,pose(frame.firstArm()));parts.add(SimPartialModels.ABSORBER_ARM,pose(frame.secondArm()));
    }
    private static PoseStack pose(Matrix4fc transform){final var pose=new PoseStack();pose.mulPose(transform);return pose;}
}
