package dev.simulated_team.simulated.content.blocks.portable_engine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import dev.simulated_team.simulated.fabric.registry.NativePartTransforms;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Source hatch, independent pipe/outlet pulses, heated overlays and current rotating shaft. */
public final class PortableEngineRenderer extends NativePartsRenderer<PortableEngineBlockEntity> {
    private static final int FULL_BRIGHT=0xF000F0;
    public PortableEngineRenderer(BlockEntityRendererProvider.Context context) { }
    protected static float getHatchOpenProgress(PortableEngineBlockEntity entity,float partial) {
        return Mth.sin(entity.getHatchOpenTime(partial)/10*Mth.HALF_PI);
    }
    @Override protected void extractParts(PortableEngineBlockEntity entity,float partial,Parts parts) {
        final var state=entity.getBlockState();final var facing=state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        final var shaft=rotating(entity,partial,rotationAxis(entity));NativePartTransforms.partialFacing(shaft,facing);parts.add(AllPartialModels.SHAFT_HALF,shaft);
        final float heat=entity.visualStrength.getValue(partial);final float hatch=getHatchOpenProgress(entity,partial);
        final boolean lit=state.getValue(BlockStateProperties.LIT);
        var models=SimPartialModels.ENGINE_PARTS;
        hatch(entity,parts,facing,models,hatch,255,!lit,false,parts.light());
        pipes(entity,parts,facing,models,partial,255,false,parts.light());
        if(heat>0) {
            models=entity.isSuperHeated()?SimPartialModels.ENGINE_PARTS_SUPERHEATED:SimPartialModels.ENGINE_PARTS_HEATED;
            pipes(entity,parts,facing,models,partial,(int)(heat*255),true,FULL_BRIGHT);
        }
        if(lit)hatch(entity,parts,facing,models,hatch,(int)((1-hatch)*255),true,true,FULL_BRIGHT);
        // The unchanged Create native global behaviour renderer owns filtering/value boxes.
    }
    private static PoseStack facing(Direction facing) {
        final var pose=new PoseStack();rotateCentered(pose,Direction.Axis.Y,AngleHelper.horizontalAngle(facing));return pose;
    }
    private static void add(Parts parts,ExtraModelKey<BlockStateModel> model,PoseStack pose,int alpha,boolean overlay,boolean emissive,int light) {
        parts.add(model,pose,(alpha<<24)|0xFFFFFF,overlay?ChunkSectionLayer.TRANSLUCENT:ChunkSectionLayer.CUTOUT,emissive,light);
    }
    private static void hatch(PortableEngineBlockEntity entity,Parts parts,Direction facing,SimPartialModels.EngineParts models,
                              float progress,int alpha,boolean inner,boolean overlay,int light) {
        final boolean emissive=overlay&&!entity.isVirtual();
        final var bottom=facing(facing);bottom.translate(0,4.9f/16,3.7f/16);bottom.rotate(Axis.XP.rotation(-progress*.65f));bottom.translate(0,-4.9f/16,-3.7f/16);
        add(parts,models.hatchBottom,bottom,alpha,overlay,emissive,light);
        add(parts,models.hatchTop,facing(facing),alpha,overlay,emissive,light);
        if(inner)add(parts,models.mouth,facing(facing.getOpposite()),255,overlay,emissive,light);
    }
    private static void pipes(PortableEngineBlockEntity entity,Parts parts,Direction facing,SimPartialModels.EngineParts models,
                              float partial,int alpha,boolean overlay,int light) {
        final float time=AnimationTickHolder.getRenderTime(entity.getLevel())/20;
        final float strength=.03f*entity.visualStrength.getValue(partial);
        final float pipeScale=(float)(Math.max(Math.sin(time*7)+.65,0)-.65)*strength*1.1f+1;
        final float outletScale=(float)(Math.max(Math.sin(time*7-1.15)+.65,0)-.65)*strength+1;
        final boolean emissive=overlay&&!entity.isVirtual();
        for(boolean right:new boolean[]{true,false}) {
            final float centerX=(right?14:2)/16f;
            final var pipe=facing(facing);scaleAbout(pipe,centerX,10/16f,8/16f,pipeScale);
            add(parts,right?models.pipeRight:models.pipeLeft,pipe,alpha,overlay,emissive,light);
            final var outlet=facing(facing);scaleAbout(outlet,centerX,10/16f,8/16f,outletScale);
            final float pivotX=(right?13.6f:2.2f)/16;
            outlet.translate(pivotX,10.2f/16,11/16f);outlet.rotate(Axis.YP.rotationDegrees(right?-7.5f:7.5f));outlet.translate(-pivotX,-10.2f/16,-11/16f);
            add(parts,right?models.outletRight:models.outletLeft,outlet,alpha,overlay,emissive,light);
        }
    }
    private static void scaleAbout(PoseStack pose,float x,float y,float z,float scale) {
        pose.translate(x,y,z);pose.scale(scale,scale,scale);pose.translate(-x,-y,-z);
    }
}
