package dev.simulated_team.simulated.content.blocks.spring;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import dev.simulated_team.simulated.fabric.registry.NativeSpringGeometry;
import dev.simulated_team.simulated.index.SimRenderTypes;
import dev.simulated_team.simulated.util.SimColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;

/** Paired spring owner/plot poses are resolved during extraction; native submission owns only copied tube geometry. */
public final class SpringRenderer extends NativePartsRenderer<SpringBlockEntity> {
    public SpringRenderer(BlockEntityRendererProvider.Context context) { }
    @Override protected void extractParts(SpringBlockEntity entity,float partial,Parts parts) {
        if(!entity.isController())return;final var other=entity.getPairedSpring();if(other==null)return;
        final var client=Minecraft.getInstance();final var block=entity.getBlockState();final var size=block.getValue(SpringBlock.SIZE);
        final ClientSubLevelContainer container=SubLevelContainer.getContainer(client.level);
        final var partnerId=entity.getPartnerSubLevelID();
        final var partner=partnerId!=null&&container!=null?(ClientSubLevel)container.getSubLevel(partnerId):null;
        final var owner=Sable.HELPER.getContainingClient(entity);final var ownerPose=owner==null?null:owner.renderPose(partial);final var partnerPose=partner==null?null:partner.renderPose(partial);
        final var center=entity.getCenter();final var otherCenter=other.getCenter();
        final var facing=block.getValue(SpringBlock.FACING);final var otherFacing=other.getBlockState().getValue(SpringBlock.FACING);
        final var normalB=new Vector3d(otherFacing.getStepX(),otherFacing.getStepY(),otherFacing.getStepZ());
        if(partnerPose!=null){partnerPose.transformNormal(normalB);partnerPose.transformPosition(otherCenter);}
        if(ownerPose!=null){ownerPose.transformNormalInverse(normalB);ownerPose.transformPositionInverse(otherCenter);}
        final double renderTime=(client.player==null?entity.getLevel().getGameTime():client.player.tickCount)+partial;
        final int color=NativeSpringGeometry.stressColor(center.distance(otherCenter),entity.getRenderLength(partial),entity.getSnappingDistance(),renderTime,SimColors.STRESSED_RED);
        final float width=switch(size){case SMALL->6;case MEDIUM->8;case LARGE->10;},textureWidth=size==SpringBlock.Size.LARGE?32:16;
        final var geometry=NativeSpringGeometry.extract(new NativeSpringGeometry.Frame(otherCenter.sub(center,new Vector3d()),facing,otherFacing,normalB,
                ownerPose==null?new Quaterniond():ownerPose.orientation(),partnerPose==null?new Quaterniond():partnerPose.orientation(),entity.getRenderLength(partial),width,textureWidth,parts.light(),color));
        if(geometry.vertices().isEmpty())return;
        final var pos=entity.getBlockPos();final var pose=new PoseStack();pose.translate(center.x-pos.getX(),center.y-pos.getY(),center.z-pos.getZ());
        final String name=(size==SpringBlock.Size.MEDIUM?"":size.getSerializedName()+"_")+"spring";
        parts.addGeometry(SimRenderTypes.spring(Identifier.fromNamespaceAndPath("simulated","textures/block/spring/"+name+".png")),geometry,pose);
    }
    @Override public boolean shouldRender(SpringBlockEntity entity,Vec3 camera){return true;}
}
