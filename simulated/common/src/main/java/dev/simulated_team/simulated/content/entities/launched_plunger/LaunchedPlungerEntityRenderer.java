package dev.simulated_team.simulated.content.entities.launched_plunger;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.mixinterface.entity.entity_rendering.EntityRenderStateExtension;
import dev.simulated_team.simulated.content.items.plunger_launcher.PlungerLauncherItemRenderer;
import dev.simulated_team.simulated.fabric.client.NativeItemFocus;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import dev.simulated_team.simulated.fabric.registry.NativePlungerRope;
import dev.simulated_team.simulated.fabric.registry.NativeVertexSnapshot;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.index.SimRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;

/** Plunger parts and the animated, lit spline rope cross native extraction as detached geometry. */
public final class LaunchedPlungerEntityRenderer extends EntityRenderer<LaunchedPlungerEntity,LaunchedPlungerEntityRenderer.State> {
    public static final class State extends EntityRenderState {
        NativePartsRenderer.State body;
        NativeVertexSnapshot rope=new NativeVertexSnapshot(List.of());
        Vec3 ropeOrigin=Vec3.ZERO;
    }
    public LaunchedPlungerEntityRenderer(EntityRendererProvider.Context context){super(context);}
    @Override public State createRenderState(){return new State();}
    @Override protected boolean affectedByCulling(LaunchedPlungerEntity entity){return false;}
    public static Vec3 getFirstPersonFocusPos(float partial){return NativeItemFocus.world(PlungerLauncherItemRenderer.FOCUS_ID);}
    private static Vec3 project(Vec3 position,float partial) {
        final var owner=Sable.HELPER.getContainingClient(position);return owner==null?position:owner.renderPose(partial).transformPosition(position);
    }
    @Override public void extractRenderState(LaunchedPlungerEntity entity,State state,float partial) {
        super.extractRenderState(entity,state,partial);
        final var other=entity.getOther();final var direction=entity.getData(LaunchedPlungerEntity.PLUNGED_DIRECTION);
        Vec3 normal,perpendicular;
        if(entity.isPlunged()){normal=Vec3.atLowerCornerOf(direction.getUnitVec3i());perpendicular=Vec3.atLowerCornerOf((direction.getAxis().isHorizontal()?Direction.UP:Direction.NORTH).getUnitVec3i());}
        else {normal=Entity.calculateViewVector(-Mth.lerp(partial,entity.xRotO,entity.getXRot()),-Mth.lerp(partial,entity.yRotO,entity.getYRot())).reverse();
            perpendicular=Entity.calculateViewVector(-Mth.lerp(partial,entity.xRotO,entity.getXRot()),-Mth.lerp(partial,entity.yRotO,entity.getYRot())-90).reverse();}
        final var owner=Sable.HELPER.getContainingClient(entity.position());
        if(owner!=null){normal=owner.renderPose(partial).transformNormal(normal);perpendicular=owner.renderPose(partial).transformNormal(perpendicular);}
        final Vec3 origin=project(new Vec3(entity.xo,entity.yo,entity.zo),partial).lerp(project(entity.position(),partial),partial);
        final Vec3 start=origin.add(normal.scale(.6));final Vec3 target;
        if(other!=null) {
            var otherNormal=other.isPlunged()?Vec3.atLowerCornerOf(other.getData(LaunchedPlungerEntity.PLUNGED_DIRECTION).getUnitVec3i())
                    :Entity.calculateViewVector(-Mth.lerp(partial,other.xRotO,other.getXRot()),-Mth.lerp(partial,other.yRotO,other.getYRot())).reverse();
            Vec3 oldPosition=new Vec3(other.xo,other.yo,other.zo),newPosition=other.position();
            if(other.isRemoved()){oldPosition=entity.getData(LaunchedPlungerEntity.TARGET_POS);newPosition=oldPosition;}
            final var targetOwner=Sable.HELPER.getContainingClient(newPosition);
            if(targetOwner!=null)otherNormal=targetOwner.renderPose(partial).transformNormal(otherNormal);
            target=project(oldPosition,partial).lerp(project(newPosition,partial),partial).add(otherNormal.scale(.6));
        } else if(entity.getData(LaunchedPlungerEntity.OTHER_PLUNGER).isEmpty()&&entity.getOwner()==Minecraft.getInstance().player
                &&Minecraft.getInstance().options.getCameraType().isFirstPerson())target=getFirstPersonFocusPos(partial);
        else if(entity.getOwner() instanceof AbstractClientPlayer player&&entity.getData(LaunchedPlungerEntity.OTHER_PLUNGER).isEmpty()) {
            float yaw=Mth.lerp(partial,player.yHeadRotO,player.yHeadRot);final float pitch=Mth.lerp(partial,player.xRotO,player.getXRot());
            yaw=Mth.lerp(Math.abs(Mth.map(pitch,90,0,1,0)),yaw,player.getPreciseBodyRotation(partial));
            final var view=Entity.calculateViewVector(pitch,yaw);final var hand=Entity.calculateViewVector(0,yaw+90);
            target=player.getPosition(partial).add(0,1.28,0).add(view.scale(.875)).add(hand.scale(Math.abs(Mth.map(pitch,90,0,.325f,0))));
        } else target=Vec3.ZERO;
        state.ropeOrigin=origin;state.rope=new NativeVertexSnapshot(List.of());
        if((entity.getData(LaunchedPlungerEntity.IS_FIRST)||other==null||other.isRemoved())&&!target.equals(Vec3.ZERO)) {
            final var points=new ArrayList<Vec3>();final var toTarget=target.subtract(start);final var directionToTarget=toTarget.normalize();
            final float length=(float)origin.distanceTo(target),time=entity.tickCount+partial+entity.getAnimationOffset();points.add(start);
            if(length<1000)for(float distance=.01f;distance<length;distance+=.5f) {
                final float delta=distance/length;final var wobble=new Vector3f();
                wobble.set(Math.cos(time/10+distance)*(1-Math.abs(directionToTarget.x)),Math.cos(time/10+distance)*(1-Math.abs(directionToTarget.y)),Math.cos(time/10+distance/2)*(1-Math.abs(directionToTarget.z)));
                wobble.add((float)(Math.sin(time/10+distance/4)*(1-Math.abs(directionToTarget.x))*2),(float)(Math.sin(time/10+distance/4)*(1-Math.abs(directionToTarget.y))),(float)(Math.sin(time/10+distance/4)*(1-Math.abs(directionToTarget.z))));
                wobble.mul(Math.max(0,1-(entity.tickCount+partial)/40-((entity.getPlungedTime()+(entity.getPlungedTime()>0?partial:0))/8)));
                wobble.mul((float)(1-Math.pow(2*delta-1,2)));points.add(start.add(toTarget.scale(delta)).subtract(wobble.x,wobble.y,wobble.z));
            }
            points.add(target);points.add(target);state.rope=NativePlungerRope.extract(points,(ClientLevel)entity.level(),origin);
        }
        final var parts=new NativePartsRenderer.Parts(state.lightCoords);final var pose=new PoseStack();
        if(entity.isPlunged())pose.rotate(direction.getRotation());
        else {pose.rotate(Axis.YP.rotationDegrees(Mth.lerp(partial,entity.yRotO,entity.getYRot())-90));pose.rotate(Axis.ZP.rotationDegrees(Mth.lerp(partial,entity.xRotO,entity.getXRot())));pose.rotate(Axis.ZP.rotationDegrees(90));}
        pose.rotate(Axis.XP.rotationDegrees(-90));pose.scale(1.75f,1.75f,1.75f);pose.translate(0,0,2.5f/16);pose.rotate(Axis.ZP.rotationDegrees(90));
        parts.add(SimPartialModels.LAUNCHED_PLUNGER_BODY,pose);
        final var faceNormal=new Vector3f((float)normal.x,(float)normal.y,(float)normal.z);
        final var toward=new Vector3f((float)(target.x-start.x),(float)(target.y-start.y),(float)(target.z-start.z)).normalize();
        float jointAngle=entity.isPlunged()?new Vector3f((float)perpendicular.x,(float)perpendicular.y,(float)perpendicular.z).angleSigned(toward,faceNormal)+(float)Math.PI/2:0;
        if(!Float.isFinite(jointAngle))jointAngle=0;
        pose.rotate(Axis.ZP.rotation(jointAngle));parts.add(SimPartialModels.LAUNCHED_PLUNGER_JOINT,pose);pose.translate(0,0,3/16f);
        pose.rotate(Axis.XP.rotationDegrees((float)start.distanceTo(target)*90*2.6f));parts.add(SimPartialModels.LAUNCHED_PLUNGER_SPOOL,pose);
        state.body=NativePartsRenderer.State.from(parts);
    }
    @Override public void submit(State state,PoseStack pose,SubmitNodeCollector nodes,CameraRenderState camera) {
        super.submit(state,pose,nodes,camera);
        if(!state.rope.vertices().isEmpty()) {
            final var ropePose=new PoseStack();ropePose.mulPose(pose.last().pose());
            final var frame=((EntityRenderStateExtension)(EntityRenderState)state).sable$getRenderTransform();final var extra=new PoseStack();
            if(frame.containingOrientation()!=null)extra.rotate(frame.containingOrientation());frame.applyEntity(extra);
            ropePose.mulPose(new Matrix4f(extra.last().pose()).invert());ropePose.translate(state.ropeOrigin.subtract(new Vec3(state.x,state.y,state.z)));
            nodes.submitCustomGeometry(ropePose,SimRenderTypes.rope(),state.rope);
        }
        if(state.body!=null)NativePartsRenderer.submitParts(state.body,pose,nodes);
    }
}
