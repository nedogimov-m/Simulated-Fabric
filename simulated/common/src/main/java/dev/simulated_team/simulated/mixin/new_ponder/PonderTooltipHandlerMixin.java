package dev.simulated_team.simulated.mixin.new_ponder;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.ponder.new_ponder_tooltip.NewPonderTooltipManager;
import dev.simulated_team.simulated.service.SimConfigService;
import net.createmod.catnip.lang.LangBuilder;
import net.createmod.ponder.foundation.PonderTooltipHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

@Mixin(PonderTooltipHandler.class)
public class PonderTooltipHandlerMixin {
	@Inject(method = "addToTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;getVisualOrderText()Lnet/minecraft/util/FormattedCharSequence;"))
	private static void simulated$addToTooltip(List<Component> tooltip, ItemStack stack, CallbackInfo ci, @Local Component component) {
		if(SimConfigService.INSTANCE.client().itemConfig.showNewPonderTag.get() && stack != null) {
			if(!NewPonderTooltipManager.hasWatchedAllScenes(stack.getItem())) {
				((MutableComponent) component).append(" ").append(SimLang.translate("tooltip.new_ponder").style(ChatFormatting.GOLD).component());
			}
		}

	}

}
