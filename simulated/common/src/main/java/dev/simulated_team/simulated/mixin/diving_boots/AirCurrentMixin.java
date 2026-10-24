package dev.simulated_team.simulated.mixin.diving_boots;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.equipment.armor.DivingBootsItem;
import com.simibubi.create.content.kinetics.fan.AirCurrent;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AirCurrent.class)
public class AirCurrentMixin {


	@WrapMethod(method = "isPlayerCreativeFlying")
	private static boolean simulated$testDivingBoots(final Entity entity, final Operation<Boolean> original) {
		if (DivingBootsItem.isWornBy(entity)) {
			return true;
		}

		return original.call(entity);
	}
}
