package dev.simulated_team.simulated.content.physics_staff;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.simulated_team.simulated.SimulatedClient;
import dev.simulated_team.simulated.fabric.client.NativeAnimatedItemRenderer;
import dev.simulated_team.simulated.fabric.client.NativeItemFocus;
import dev.simulated_team.simulated.index.SimRenderTypes;
import dev.simulated_team.simulated.util.SimMathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3d;
import org.joml.Vector3f;
import java.util.IdentityHashMap;
import java.util.Map;

/** Native staff articulation, real emissive partials and late world-oriented cube/muzzle transforms. */
public final class PhysicsStaffItemRenderer extends NativeAnimatedItemRenderer {
    public static final PhysicsStaffItemRenderer INSTANCE=new PhysicsStaffItemRenderer();
    public static final Identifier FOCUS_ID=Identifier.parse("simulated:physics_staff");
    private record Cube(Quaternionfc orientation,float scale) {public Cube{orientation=new Quaternionf(orientation);}}
    private static final class StaffState extends AnimatedState {final Map<Draw,Cube> cubes=new IdentityHashMap<>();}
    public PhysicsStaffItemRenderer(){for(String name:new String[]{"item","core","core_glow","ring","sigma","inner_cube","outer_cube"})model(name,modelPath(name));}
    @Override protected String modelPath(String name){return "simulated:item/creative_physics_staff/"+name;}
    public static Vec3 getFirstPersonFocusPos(float partial){return NativeItemFocus.relative(FOCUS_ID);}
    @Override protected State extract(ItemStack stack,ItemDisplayContext display,float partial,float time){return extractForOwner(stack,display,partial,time,null);}
    @Override protected State extractForOwner(ItemStack stack,ItemDisplayContext display,float partial,float time,ItemOwner itemOwner) {
        final var state=new StaffState();final var client=Minecraft.getInstance();final var handler=SimulatedClient.PHYSICS_STAFF_CLIENT_HANDLER;
        float open=0,cube=0;final var player=client.player;
        final boolean held=display.firstPerson()||display==ItemDisplayContext.THIRD_PERSON_LEFT_HAND||display==ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        if(player!=null&&held) {
            final Player owner=itemOwner instanceof Player source?source:player;
            if(owner==player&&(ItemStack.isSameItemSameComponents(player.getMainHandItem(),stack)||ItemStack.isSameItemSameComponents(player.getOffhandItem(),stack))) {
                open=Mth.lerp(partial,handler.previousExtension,handler.extension);cube=Mth.lerp(partial,handler.previousCubeScale,handler.cubeScale);
            } else {
                for(var entry:handler.beams.entrySet()) {
                    final var other=client.level==null?null:client.level.getPlayerByUUID(entry.getKey());
                    if(other!=null&&(other==owner||other.getMainHandItem()==stack||other.getOffhandItem()==stack)) {
                        open=Mth.lerp(partial,entry.getValue().previousExtension,entry.getValue().extension);
                        cube=Mth.lerp(partial,entry.getValue().previousCubeScale,entry.getValue().cubeScale);break;
                    }
                }
            }
        }
        final var pose=new PoseStack();final var util=new Quaternionf();
        if(display.firstPerson()) {
            final var drag=handler.getDragSession();
            if(drag!=null&&player!=null) {
                final var anchor=((ClientSubLevel)drag.dragSubLevel()).renderPose().transformPosition(new Vector3d(drag.dragLocalAnchor()));
                anchor.sub(player.getEyePosition(partial).x,player.getEyePosition(partial).y,player.getEyePosition(partial).z);
                if(anchor.lengthSquared()>1E-12) {
                    anchor.normalize();client.gameRenderer.mainCamera().rotation().transformInverse(anchor);
                    final var aim=SimMathUtils.getQuaternionfFromVectorRotation(new Vector3d(0,0,-1),anchor);
                    pose.rotate(util.identity().rotateY(-Mth.HALF_PI));pose.rotate(aim.slerp(util.identity(),.6f));pose.rotate(util.identity().rotateY(Mth.HALF_PI));
                }
            }
            final float flip=display==ItemDisplayContext.FIRST_PERSON_LEFT_HAND?-1:1;
            final float tilt=Mth.lerp(partial,handler.previousTilt,handler.tilt);
            pose.rotate(util.identity().rotateZ((float)Math.toRadians((tilt*.5+.5)*-61)*flip));
        }
        draw(state,"item",pose);
        glowing(state,"core",pose,SimRenderTypes.itemGlowingSolid(false),0xF000F0);
        glowing(state,"core_glow",pose,SimRenderTypes.itemGlowingTranslucent(false),0xF000F0);
        pose.pushPose();pose.translate(0,6.5/16,0);draw(state,"ring",pose);pose.popPose();
        pose.translate(0,9/16.0,0);
        for(int side=0;side<2;side++){pose.pushPose();pose.rotate(Axis.YP.rotationDegrees(side*180));pose.translate(-3/16.0,0,0);pose.rotate(Axis.ZP.rotationDegrees(open*20));draw(state,"sigma",pose);pose.popPose();}
        pose.translate(0,6/16.0,0);state.focusId=FOCUS_ID;state.focus=pose.last().pose().transformPosition(new Vector3f());
        if(display.firstPerson()&&handler.getDragSession()!=null)handler.lastCubeOrientation.set(handler.getDragSession().dragOrientation());
        cube=Mth.clamp(Mth.lerp(cube,-.05f,1),0,1)*.8f;
        if(cube>1E-6f) {
            pose.scale(cube,cube,cube);glowing(state,"inner_cube",pose,SimRenderTypes.itemGlowingSolid(false),0xF000F0);
            if(display.firstPerson())state.cubes.put(state.draws.getLast(),new Cube(handler.lastCubeOrientation,cube));
            pose.scale(1.2f,1.2f,1.2f);glowing(state,"outer_cube",pose,SimRenderTypes.itemGlowingTranslucent(false),0xF000F0);
            if(display.firstPerson())state.cubes.put(state.draws.getLast(),new Cube(handler.lastCubeOrientation,cube*1.2f));
        }
        return state;
    }
    @Override protected PoseStack poseForDraw(State source,Draw draw,PoseStack parent) {
        final var posed=super.poseForDraw(source,draw,parent);final var cube=((StaffState)source).cubes.get(draw);if(cube==null)return posed;
        final var center=posed.last().pose().transformPosition(new Vector3f(.5f));
        final var world=new PoseStack();world.translate(center.x,center.y,center.z);world.rotate(cube.orientation);
        world.scale(cube.scale,cube.scale,cube.scale);world.translate(-.5,-.5,-.5);return world;
    }
}
