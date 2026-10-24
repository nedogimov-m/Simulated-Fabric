package dev.eriksonn.aeronautics.content.items;

import com.simibubi.create.content.equipment.armor.BaseArmorItem;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.index.AeroArmorMaterials;
import dev.eriksonn.aeronautics.index.AeroItems;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.equipment.ArmorType;

public class AviatorsGogglesItem extends BaseArmorItem {
	public static final ArmorType TYPE = ArmorType.HELMET;
	private static final Identifier TEXTURE = Aeronautics.path("aviators_goggles");

	public AviatorsGogglesItem(final Properties properties) {
		super(AeroArmorMaterials.AVIATORS_GOGGLES, TYPE, properties, TEXTURE);
		GogglesItem.addIsWearingPredicate(player -> player.getItemBySlot(EquipmentSlot.HEAD).is(AeroItems.AVIATORS_GOGGLES));
	}
}
