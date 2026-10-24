package dev.simulated_team.simulated.content.blocks.nav_table;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.RenderableNavigationTarget;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.index.SimTags;
import dev.simulated_team.simulated.util.SimColors;
import dev.simulated_team.simulated.util.SimDirectionUtil;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector3f;

/** Copied cardinal indicators/pointer and native item/custom target extraction on every mounting face. */
public final class NavTableRenderer extends NativePartsRenderer<NavTableBlockEntity> {
    private final ItemModelResolver items;
    public NavTableRenderer(BlockEntityRendererProvider.Context context) {this.items=context.itemModelResolver();}
    @Override protected void extractParts(NavTableBlockEntity entity,float partial,Parts parts) {
        final var block=entity.getBlockState();final var facing=block.getValue(NavTableBlock.FACING);
        final var pose=new PoseStack();pose.translate(.5,.5,.5);pose.rotate(facing.getRotation());
        final float arrow=(float)(entity.getClientTargetAngle(partial)-Math.PI/2);
        final var rotatedDirection=new Vector3f();
        for(var direction:SimDirectionUtil.Y_AXIS_PLANE) {
            facing.getRotation().transform(direction.getStepX(),direction.getStepY(),direction.getStepZ(),rotatedDirection);
            final var logical=Direction.getApproximateNearest(rotatedDirection.x,rotatedDirection.y,rotatedDirection.z);
            final float strength=entity.isPowering?Math.max(entity.getRedstoneStrength(logical),0)/15f:0;
            pose.pushPose();pose.translate(0,-.5,0);pose.rotate(Axis.YP.rotationDegrees(switch(direction){case NORTH->0;case SOUTH->180;case WEST->90;case EAST->270;default->throw new IllegalArgumentException("Expected planar indicator");}));pose.translate(0,0,.5);
            parts.add(SimPartialModels.NAV_TABLE_INDICATOR,pose,SimColors.redstone(strength),ChunkSectionLayer.CUTOUT,false,parts.light());pose.popPose();
        }
        pose.pushPose();pose.translate(0,.3,0);pose.rotate(Axis.YP.rotation(arrow));parts.add(SimPartialModels.NAV_TABLE_POINTER,pose);pose.popPose();
        final var held=entity.getHeldItem();if(held.isEmpty())return;
        final var item=new ItemStackRenderState();items.updateForTopItem(item,held,ItemDisplayContext.FIXED,entity.getLevel(),null,0);
        final boolean blockItem=item.usesBlockLight();
        pose.translate(0,.3+(blockItem?.25:.15),0);pose.rotate(Axis.XN.rotationDegrees(90));
        final float scale=blockItem?.5f:.375f;pose.scale(scale,scale,scale);
        if(held.getItem() instanceof RenderableNavigationTarget target)target.extractInNavTable(held,entity,block,partial,pose,parts,item);
        else {if(held.is(SimTags.Items.ROTATE_WITH_NAV_ARROW))pose.rotate(Axis.ZP.rotation(arrow));parts.addItem(item,pose);}
    }
}
