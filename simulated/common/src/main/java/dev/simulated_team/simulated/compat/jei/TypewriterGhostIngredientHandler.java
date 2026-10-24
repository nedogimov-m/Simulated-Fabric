package dev.simulated_team.simulated.compat.jei;

import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen.LinkedTypewriterScreen;
import dev.simulated_team.simulated.network.packets.linked_typewriter.TypewriterMenuModifySlots;
import foundry.veil.api.network.VeilPacketManager;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** JEI writes frequency samples into the real editor menu; it never moves a player's items. */
public final class TypewriterGhostIngredientHandler implements IGhostIngredientHandler<LinkedTypewriterScreen> {
    @Override
    public <I> List<Target<I>> getTargetsTyped(final LinkedTypewriterScreen screen,
                                              final ITypedIngredient<I> ingredient,final boolean doStart) {
        final var menu=screen.getMenu();
        if(!screen.modifier.modifying||!menu.slotsActive||ingredient.getItemStack().filter(stack->!stack.isEmpty()).isEmpty())
            return List.of();
        final List<Target<I>> targets=new ArrayList<>();
        for(final var slot:menu.slots) {
            if(slot.container!=menu.ghostInventory||!slot.isFake()||!slot.isActive())continue;
            final int sampleIndex=slot.getContainerSlot();
            targets.add(new Target<>() {
                @Override public Rect2i getArea() {
                    return new Rect2i(screen.getLeftPos()+slot.x,screen.getTopPos()+slot.y,16,16);
                }
                @Override public void accept(final I dropped) {
                    if(!(dropped instanceof ItemStack stack)||stack.isEmpty()||!screen.modifier.modifying
                            ||!menu.slotsActive||!slot.isActive()||screen.getMenu()!=menu)return;
                    menu.setGhostSample(sampleIndex,stack);
                    slot.setChanged();
                    VeilPacketManager.server().sendPacket(new TypewriterMenuModifySlots(
                            menu.ghostInventory.getItem(0).copy(),menu.ghostInventory.getItem(1).copy()));
                }
            });
        }
        return List.copyOf(targets);
    }

    @Override public void onComplete() { }
}
