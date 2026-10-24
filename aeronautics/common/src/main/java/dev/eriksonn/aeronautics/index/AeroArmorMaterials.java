package dev.eriksonn.aeronautics.index;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.Map;

/** Armor materials are value records in 26.3; the original protection/enchanting/repair values are retained. */
public final class AeroArmorMaterials {
    public static final ArmorMaterial AVIATORS_GOGGLES = new ArmorMaterial(5, Map.of(ArmorType.HELMET, 1),
            15, SoundEvents.ARMOR_EQUIP_LEATHER, 0, 0,
            TagKey.create(Registries.ITEM, id("repairs/aviators_goggles")),
            ResourceKey.create(EquipmentAssets.ROOT_ID, id("aviators_goggles")));
    public static void init() {}
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("aeronautics", path); }
    private AeroArmorMaterials() {}
}
