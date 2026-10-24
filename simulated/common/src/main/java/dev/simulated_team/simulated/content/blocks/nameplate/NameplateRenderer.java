package dev.simulated_team.simulated.content.blocks.nameplate;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import java.util.List;

/** Nameplate glyphs, source layout, glow/light and8x-outline state are copied before native text submission. */
public final class NameplateRenderer implements BlockEntityRenderer<NameplateBlockEntity,NameplateRenderer.State> {
    public static final class State extends BlockEntityRenderState {
        private List<FormattedCharSequence> lines=List.of();private Matrix4fc transform=new Matrix4f();private int color,outline,light;
    }
    private final Font font;
    public NameplateRenderer(BlockEntityRendererProvider.Context context){font=context.font();}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(NameplateBlockEntity entity,State state,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(entity,state,partial,camera,overlay);state.lines=List.of();
        final var block=entity.getBlockState();final var position=block.getValue(NameplateBlock.POSITION);
        if(position!=NameplateBlock.Position.LEFT&&position!=NameplateBlock.Position.SINGLE)return;
        final int pixelsTall=entity.glowing?5:6,pixelsLeft=3;
        final int available=(entity.getControllerWidth()*16-pixelsLeft*2)*7/pixelsTall+1;if(available<=0)return;
        final String trimmed=font.plainSubstrByWidth(entity.getName(),available);final int width=font.width(trimmed);if(width<=0)return;
        final var pose=new PoseStack();pose.translate(.5,.5,.5);pose.rotate(Axis.YP.rotationDegrees(-block.getValue(NameplateBlock.FACING).toYRot()+180));pose.translate(-.5,-.5,-.5);
        pose.translate(1,1,1);pose.translate(0,0,-4.05/16);pose.translate(-pixelsLeft/16f,-(16-pixelsTall)/32.0,0);
        final float scale=pixelsTall/16f;pose.scale(scale,scale,scale);pose.scale(1/7f,1/7f,1/7f);pose.rotate(Axis.ZP.rotationDegrees(180));pose.translate((available-1)/2.0-width/2.0,0,0);
        state.transform=new Matrix4f(pose.last().pose());state.lines=List.copyOf(font.split(Component.literal(trimmed),width));
        state.color=0xff000000|(entity.glowing?entity.getTextColor().getTextColor():entity.getDarkColor(entity.getTextColor()));
        state.light=entity.glowing?LightCoordsUtil.FULL_BRIGHT:state.lightCoords;
        state.outline=0;
        if(entity.glowing) {
            final var client=Minecraft.getInstance();final var player=client.player;final var view=client.getCameraEntity();
            final boolean outline=entity.getTextColor()==DyeColor.BLACK||(player!=null&&client.options.getCameraType().isFirstPerson()&&player.isScoping())
                    ||view!=null&&view.distanceToSqr(Vec3.atCenterOf(entity.getBlockPos()))<16*16;
            if(outline)state.outline=0xff000000|entity.getDarkColor(entity.getTextColor());
        }
    }
    @Override public void submit(State state,PoseStack pose,SubmitNodeCollector nodes,CameraRenderState camera) {
        if(state.lines.isEmpty())return;pose.pushPose();pose.mulPose(state.transform);
        for(var line:state.lines)nodes.submitText(pose,0,0,line,false,Font.DisplayMode.NORMAL,state.light,state.color,0,state.outline);
        pose.popPose();
    }
}
