package dev.simulated_team.simulated.content.blocks.nav_table.navigation_target;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Native custom target geometry is extracted at the item pedestal, then submitted without a live owner. */
public interface RenderableNavigationTarget extends NavigationTarget {
    default void extractInNavTable(ItemStack self,NavTableBlockEntity owner,BlockState state,float partial,PoseStack pose,
                                   NativePartsRenderer.Parts parts,ItemStackRenderState defaultItem) {
        parts.addItem(defaultItem,pose);
    }
}
