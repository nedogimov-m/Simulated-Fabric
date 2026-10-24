package dev.simulated_team.simulated.content.blocks.throttle_lever;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.util.SimColors;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.BlockHitResult;
import java.util.ArrayList;
import java.util.List;

/** Handle/button/diode extraction, source grip transform and detached native hover outline. */
public final class ThrottleLeverRenderer extends NativePartsRenderer<ThrottleLeverBlockEntity> {
    protected static final double ANGLE_LIMIT=40;
    public ThrottleLeverRenderer(BlockEntityRendererProvider.Context context) { }
    private static float angle(float value,AttachFace face) {
        final float angle=(float)Math.toRadians((value/15)*(ANGLE_LIMIT*2)-ANGLE_LIMIT);
        return face==AttachFace.WALL?-angle:angle;
    }
    public static void transformHandleExternal(ThrottleLeverBlockEntity entity,float partial,PoseStack pose) {
        final var state=entity.getBlockState();initial(pose,state);
        pivot(pose,angle(entity.clientAngle.getValue(partial),state.getValue(FaceAttachedHorizontalDirectionalBlock.FACE)));
    }
    private static void initial(PoseStack pose,BlockState state) {
        final var attached=state.getValue(FaceAttachedHorizontalDirectionalBlock.FACE);
        final var facing=state.getValue(FaceAttachedHorizontalDirectionalBlock.FACING);
        rotateCentered(pose,Direction.Axis.Y,AngleHelper.horizontalAngle(facing));
        rotateCentered(pose,Direction.Axis.X,attached==AttachFace.FLOOR?0:attached==AttachFace.WALL?90:180);
        if(attached==AttachFace.CEILING)rotateCentered(pose,Direction.Axis.Y,180);
    }
    private static void pivot(PoseStack pose,float angle) {
        pose.translate(.5,3/16f,.5);pose.rotate(Axis.XP.rotation(angle));pose.translate(-.5,-3/16f,-.5);
    }
    private static PoseStack handle(BlockState state,float angle) {
        final var pose=new PoseStack();initial(pose,state);pivot(pose,angle);
        if(state.getValue(FaceAttachedHorizontalDirectionalBlock.FACE)==AttachFace.WALL)rotateCentered(pose,Direction.Axis.Y,180);
        return pose;
    }
    @Override protected void extractParts(ThrottleLeverBlockEntity entity,float partial,Parts parts) {
        final var state=entity.getBlockState();final var face=state.getValue(FaceAttachedHorizontalDirectionalBlock.FACE);
        final float angle=angle(entity.clientAngle.getValue(partial),face);
        parts.add(SimPartialModels.THROTTLE_LEVER_HANDLE,handle(state,angle));
        final var button=handle(state,angle);button.translate(0,14/16f,8/16f);
        button.rotate(Axis.XP.rotationDegrees(entity.clientPressedLerp.getValue(partial)*-7));button.translate(0,-14/16f,-8/16f);
        parts.add(SimPartialModels.THROTTLE_LEVER_BUTTON,button);
        final var diode=new PoseStack();initial(diode,state);
        parts.add(SimPartialModels.THROTTLE_LEVER_DIODE,diode,SimColors.redstone(Math.max(0,entity.state/15f)),ChunkSectionLayer.CUTOUT,false,parts.light());
        final var client=Minecraft.getInstance();
        if(!entity.isVirtual()&&client.hitResult instanceof BlockHitResult hit&&hit.getBlockPos().equals(entity.getBlockPos())) {
            final var outline=new PoseStack();initial(outline,state);pivot(outline,angle);
            final var edges=new ArrayList<Edge>();
            SimBlocks.THROTTLE_LEVER.getHandleShape(SimBlocks.THROTTLE_LEVER.defaultBlockState())
                    .forAllEdges((x1,y1,z1,x2,y2,z2)->edges.add(new Edge((float)x1,(float)y1,(float)z1,(float)x2,(float)y2,(float)z2)));
            final List<Edge> copied=List.copyOf(edges);
            final float lineWidth=Math.max(2.5f,client.getWindow().getWidth()/1920f*2.5f);
            parts.addGeometry(RenderTypes.lines(),(pose,vertices)->{for(var edge:copied)edge.render(pose,vertices,lineWidth);},outline);
        }
    }
    private record Edge(float x1,float y1,float z1,float x2,float y2,float z2) {
        void render(PoseStack.Pose pose,VertexConsumer out,float lineWidth) {
            float x=x2-x1,y=y2-y1,z=z2-z1;float length=(float)Math.sqrt(x*x+y*y+z*z);x/=length;y/=length;z/=length;
            out.addVertex(pose,x1,y1,z1).setColor(0x66000000).setNormal(pose,x,y,z).setLineWidth(lineWidth);
            out.addVertex(pose,x2,y2,z2).setColor(0x66000000).setNormal(pose,x,y,z).setLineWidth(lineWidth);
        }
    }
}
