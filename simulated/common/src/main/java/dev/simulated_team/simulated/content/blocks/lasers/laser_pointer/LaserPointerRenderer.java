package dev.simulated_team.simulated.content.blocks.lasers.laser_pointer;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.simulated_team.simulated.content.blocks.lasers.AbstractLaserRenderer;
import dev.simulated_team.simulated.content.blocks.lasers.LaserBehaviour;
import dev.simulated_team.simulated.fabric.registry.NativePartTransforms;
import dev.simulated_team.simulated.fabric.registry.NativePartialGeometry;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.index.SimRenderTypes;
import dev.simulated_team.simulated.util.SimColors;
import net.minecraft.util.Util;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.joml.Vector3d;
import org.joml.Vector4f;

import java.awt.*;

public class LaserPointerRenderer extends AbstractLaserRenderer<LaserPointerBlockEntity> {

    public LaserPointerRenderer(final BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void extractParts(final LaserPointerBlockEntity blockEntity, final float partialTicks, final Parts parts) {
        final Vector4f colors = this.getColors(blockEntity, partialTicks);
        final boolean isDarkerThanDark = colors.x == 0 && colors.y == 0 && colors.z == 0;
        final var key=blockEntity.shouldCast()&&!isDarkerThanDark?SimPartialModels.LASER_POINTER_LENS_ON:SimPartialModels.LASER_POINTER_LENS_OFF;
        final int color=0xff000000|((int)(colors.x*255)<<16)|((int)(colors.z*255)<<8)|(int)(colors.y*255);
        final var lens=NativePartialGeometry.extract(key,color,blockEntity.shouldCast()?LightCoordsUtil.FULL_BRIGHT:parts.light());
        final var pose=new PoseStack();NativePartTransforms.faceNorth(pose,blockEntity.getBlockState().getValue(LaserPointerBlock.FACING));
        parts.addGeometry(SimRenderTypes.lens(),lens,pose);

        // only draw non-black lasers
        if (!isDarkerThanDark) {
            super.extractParts(blockEntity, partialTicks, parts);
        }
    }

    @Override
    public float getLaserScale(final LaserBehaviour laser) {
        return 0.48f;
    }

    @Override
    public Vector4f getColors(final LaserPointerBlockEntity blockEntity, final float partialTicks) {
        Color c = new Color(blockEntity.laserColor);
        if (blockEntity.isRainbow()) {
            final Vector3d baseLCh = SimColors.LabToLCh(SimColors.toOklab(c));
            final float t;
            if (blockEntity.isVirtual()) {
                t = (float) ((Util.getMillis() % 5000) * 2 * Math.PI / 5000);
            } else {
                final long timeOff = blockEntity.getLevel().getGameTime();
                t = (float) (((timeOff) % 100 + partialTicks) * 2 * Math.PI / 100f);
            }
            c = SimColors.LChOklab(0.8f, 0.3f, (float) (t + baseLCh.z()));
        }
        // 25% opacity at strongest
        return new Vector4f(c.getRed() / 255f, c.getBlue() / 255f, c.getGreen() / 255f, blockEntity.getPower() / 60f);
    }
}
