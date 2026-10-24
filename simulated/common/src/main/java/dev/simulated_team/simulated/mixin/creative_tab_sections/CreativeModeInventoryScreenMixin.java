package dev.simulated_team.simulated.mixin.creative_tab_sections;

import com.llamalad7.mixinextras.sugar.Local;
import dev.simulated_team.simulated.client.sections.SimulatedSection;
import dev.simulated_team.simulated.index.SimResourceManagers;
import dev.simulated_team.simulated.fabric.registry.NativeItemCatalog;
import dev.simulated_team.simulated.registrate.simulated_tab.SimulatedCreativeTab;
import dev.simulated_team.simulated.service.SimTabService;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(CreativeModeInventoryScreen.class)
public class CreativeModeInventoryScreenMixin {
	@Shadow private static CreativeModeTab selectedTab;

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void simulated$render(final GuiGraphicsExtractor guiGraphics, final int mouseX, final int mouseY, final float partialTick, final CallbackInfo ci) {
		if (selectedTab == SimTabService.INSTANCE.getCreativeTab()) {
			SimulatedCreativeTab.renderBanners((CreativeModeInventoryScreen) (Object) this, guiGraphics, mouseX, mouseY);
		}
	}

    @Inject(method="getTooltipFromContainerItem",at=@At("RETURN"),cancellable=true)
    private void simulated$sectionTooltip(ItemStack stack,CallbackInfoReturnable<List<Component>> result) {
        var sectionId=NativeItemCatalog.sectionOf(stack.getItem());
        if(sectionId==null)return;
        var section=SimResourceManagers.SIMULATED_SECTION.get(sectionId);
        if(section==null)return;
        var lines=new java.util.ArrayList<>(result.getReturnValue());
        lines.add(Math.min(1,lines.size()),section.title().text().copy().withStyle(ChatFormatting.BLUE));
        result.setReturnValue(lines);
    }
}
