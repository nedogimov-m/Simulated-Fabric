package dev.simulated_team.simulated.content.items.plunger_launcher;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.equipment.zapper.ShootableGadgetRenderHandler;
import com.simibubi.create.foundation.particle.AirParticleData;
import dev.simulated_team.simulated.content.entities.launched_plunger.LaunchedPlungerEntityRenderer;
import dev.simulated_team.simulated.fabric.client.NativeAnimatedItemRenderer;
import dev.simulated_team.simulated.index.SimItems;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.mixin_interface.PlayerLaunchedPlungerExtension;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Native launcher body/two staged reload plungers plus actual hand-pass muzzle capture. */
public final class PlungerLauncherItemRenderer extends NativeAnimatedItemRenderer {
    public static final Identifier FOCUS_ID=Identifier.parse("simulated:plunger_launcher");
    public static final PlungerLauncherItemRenderer INSTANCE=new PlungerLauncherItemRenderer();
    public PlungerLauncherItemRenderer(){for(String name:new String[]{"item","body","spool","joint"})model(name,modelPath(name));}
    @Override protected String modelPath(String name) {
        return switch(name){case "body"->"simulated:item/plunger_launcher/plunger_tether";case "spool"->"simulated:item/plunger_launcher/tether_spool";case "joint"->"simulated:item/plunger_launcher/spool_joint";default->"simulated:item/plunger_launcher/item";};
    }
    @Override protected State extract(ItemStack stack,ItemDisplayContext display,float partial,float time) {
        final var state=new AnimatedState();final var pose=new PoseStack();pose.scale(.8f,.8f,.8f);pose.translate(0,0,.15f);draw(state,"item",pose);
        final var player=Minecraft.getInstance().player;
        final var plunger=player instanceof PlayerLaunchedPlungerExtension owner?owner.simulated$getLaunchedPlunger():null;
        final float cooldown=player==null?0:player.getCooldowns().getCooldownPercent(stack,partial);
        if(cooldown<=.6f||(plunger!=null&&plunger.getOther()==null)) {
            if((plunger==null||plunger.isRemoved()||plunger.getOther()!=null)&&cooldown<=.4f)plunger(state,pose,cooldown,true);
            plunger(state,pose,cooldown,false);
        }
        pose.translate(2/16f,-1/16f,-4/16f);state.focusId=FOCUS_ID;state.focus=pose.last().pose().transformPosition(new Vector3f());
        return state;
    }
    private void plunger(AnimatedState state,PoseStack pose,float cooldown,boolean first) {
        pose.pushPose();pose.translate(2/16f*(first?-1:1),-1/16f,-5/16f);
        if(cooldown>0&&PlungerLauncherItem.reloadCooldown) {
            final float slide=Mth.clamp(Mth.map(cooldown,first?.1f:.3f,first?.4f:.6f,0,1),0,1);
            pose.translate(0,0,-slide*slide*slide/12f);
        }
        draw(state,"body",pose);draw(state,"joint",pose);pose.translate(0,0,3/16f);draw(state,"spool",pose);pose.popPose();
    }
    public static final class RenderHandler extends ShootableGadgetRenderHandler {
        public void basicShoot(InteractionHand hand) {
            final var player=Minecraft.getInstance().player;if(player==null)return;
            final boolean right=hand==InteractionHand.MAIN_HAND^player.getMainArm()==HumanoidArm.LEFT;
            if(right){rightHandAnimation=.2f;dontReequipRight=false;}else{leftHandAnimation=.2f;dontReequipLeft=false;}
            final Vec3 focus=LaunchedPlungerEntityRenderer.getFirstPersonFocusPos(0);
            for(int i=0;i<Math.random()*4;i++) {
                final var direction=VecHelper.offsetRandomly(player.getViewVector(0),player.level().getRandom(),.5f);
                player.level().addParticle(new AirParticleData(1,.25f),focus.x,focus.y,focus.z,direction.x,direction.y,direction.z);
            }
            playSound(hand,player.position());
        }
        @Override public void playSound(InteractionHand hand,Vec3 position) { }
        @Override protected boolean appliesTo(ItemStack stack){return stack.is(SimItems.PLUNGER_LAUNCHER);}
        @Override protected void transformTool(PoseStack pose,float flip,float equip,float recoil,float partial){pose.translate(flip*-.1f,.05f,.14f);pose.rotate(Axis.XP.rotationDegrees(recoil*80));}
        @Override protected void transformHand(PoseStack pose,float flip,float equip,float recoil,float partial){pose.scale(0,0,0);}
    }
}
