package dev.ryanhcode.offroad.index;

import dev.ryanhcode.offroad.content.entities.BoreheadContraptionEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** Native common entity registration, with the original zero eye height and tracking policy. */
public final class OffroadEntityTypes {
    public static final EntityType<BoreheadContraptionEntity> BOREHEAD_CONTRAPTION_ENTITY;
    static {
        final ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath("offroad", "borehead_contraption_entity"));
        BOREHEAD_CONTRAPTION_ENTITY = Registry.register(BuiltInRegistries.ENTITY_TYPE, key,
                EntityType.Builder.of(BoreheadContraptionEntity::new, MobCategory.MISC)
                        .clientTrackingRange(20).updateInterval(40).sized(1, 1).eyeHeight(0)
                        .fireImmune().noLootTable().build(key));
    }
    public static void init() {}
    private OffroadEntityTypes() {}
}
