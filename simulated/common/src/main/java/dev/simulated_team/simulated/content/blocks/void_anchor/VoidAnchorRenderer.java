package dev.simulated_team.simulated.content.blocks.void_anchor;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.simulated_team.simulated.content.end_sea.EndSeaShadowRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public final class VoidAnchorRenderer implements BlockEntityRenderer<VoidAnchorBlockEntity,VoidAnchorRenderer.State> {
    public static final class State extends BlockEntityRenderState { Vector3d position; }
    public VoidAnchorRenderer(BlockEntityRendererProvider.Context context){}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(VoidAnchorBlockEntity entity,State state,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay breaking){
        BlockEntityRenderer.super.extractRenderState(entity,state,partial,camera,breaking);
        state.position=new Vector3d(Sable.HELPER.projectOutOfSubLevel(entity.getLevel(),JOMLConversion.atCenterOf(entity.getBlockPos())));
    }
    @Override public void submit(State state,PoseStack pose,SubmitNodeCollector nodes,CameraRenderState camera){
        if(state.position!=null)EndSeaShadowRenderer.addVoidAnchor(state.position);
    }
    @Override public boolean shouldRender(VoidAnchorBlockEntity entity,Vec3 camera){return true;}
    @Override public boolean shouldRenderOffScreen(){return true;}
    @Override public int getViewDistance(){return 512;}
}
