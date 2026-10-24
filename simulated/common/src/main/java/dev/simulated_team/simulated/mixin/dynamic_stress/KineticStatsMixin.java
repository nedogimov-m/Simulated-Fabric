package dev.simulated_team.simulated.mixin.dynamic_stress;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipHelper;
import dev.simulated_team.simulated.api.CustomStressImpactTooltipProvider;
import dev.simulated_team.simulated.data.SimLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import java.util.*;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(KineticStats.class)
public final class KineticStatsMixin {
    @WrapMethod(method="getKineticStats",remap=false)
    private static List<Component> simulated$dynamicImpact(Block block,Player player,Operation<List<Component>> original){
        var originalLines=original.call(block,player);
        if(!(block instanceof CustomStressImpactTooltipProvider provider))return originalLines;
        var lines=new ArrayList<>(originalLines);
        var config=AllConfigs.server().kinetics;
        double impact=BlockStressValues.getImpact(block);
        ChatFormatting color=impact>=config.highStressImpact.get()?ChatFormatting.RED:impact>=config.mediumStressImpact.get()?ChatFormatting.GOLD:ChatFormatting.YELLOW;
        for(int i=0;i+1<lines.size();i++)if(lines.get(i).getContents() instanceof TranslatableContents text&&text.getKey().equals("create.tooltip.stressImpact")){
            lines.set(i,SimLang.space().translate("tooltip.dynamic_stress_impact").style(color).component());
            var value=Component.literal(TooltipHelper.makeProgressBar(provider.getBarLength(),provider.getFilledBarLength())).withStyle(color);
            if(player!=null&&GogglesItem.isWearingGoggles(player))value.append(" x ").append(provider.getCustomImpactLang().component());
            else value.append(Component.translatable("create.tooltip.stressImpact."+(impact>=config.highStressImpact.get()?"high":impact>=config.mediumStressImpact.get()?"medium":"low")));
            lines.set(i+1,value);break;
        }
        return lines;
    }
}
