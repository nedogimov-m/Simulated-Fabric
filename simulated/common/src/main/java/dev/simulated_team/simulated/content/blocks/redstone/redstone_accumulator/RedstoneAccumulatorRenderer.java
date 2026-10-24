package dev.simulated_team.simulated.content.blocks.redstone.redstone_accumulator;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.simulated_team.simulated.fabric.registry.NativePartialGeometry;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.index.SimRenderTypes;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Alpha carries source gamma-corrected lamp progress, while the native diode shader emits actual texture opacity. */
public final class RedstoneAccumulatorRenderer extends NativePartsRenderer<RedstoneAccumulatorBlockEntity> {
    public static final Identifier SHADER_NAME=Identifier.parse("simulated:native/diode");
    public static final RenderType DIODE_RENDER_TYPE=SimRenderTypes.diode();
    public RedstoneAccumulatorRenderer(BlockEntityRendererProvider.Context context) { }
    @Override protected void extractParts(RedstoneAccumulatorBlockEntity entity,float partial,Parts parts) {
        final int color=getLitAmount(entity,partial)<<24|0xffffff;
        final var geometry=NativePartialGeometry.extract(SimPartialModels.REDSTONE_ACCUMULATOR_DIODE,color,parts.light());
        final var pose=new PoseStack();pose.translate(.5,0,.5);
        pose.rotate(Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(entity.getBlockState().getValue(RedstoneAccumulatorBlock.FACING))));
        parts.addGeometry(DIODE_RENDER_TYPE,geometry,pose);
    }
    private static int getLitAmount(RedstoneAccumulatorBlockEntity entity,float partial) {
        final float progress=1-(float)Math.pow(entity.lerpedState.getValue(partial)/15f,1.5);
        return (int)Mth.clamp(progress*255,0,255);
    }
}
