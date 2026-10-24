package dev.eriksonn.aeronautics.index;

import dev.eriksonn.aeronautics.content.blocks.hot_air.gust.GustEntity;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.contraption.PropellerBearingContraptionEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** Common native entity types; both original entity eye heights are explicitly zero. */
public final class AeroEntityTypes {
    public static final EntityType<PropellerBearingContraptionEntity> PROPELLER_CONTROLLED_CONTRAPTION = register(
            "propeller_bearing_contraption", EntityType.Builder.of(PropellerBearingContraptionEntity::new, MobCategory.MISC)
                    .clientTrackingRange(20).updateInterval(40).sized(1, 1).eyeHeight(0).fireImmune());
    public static final EntityType<GustEntity> GUST = register("gust", EntityType.Builder.<GustEntity>of(GustEntity::new, MobCategory.MISC)
            .clientTrackingRange(20).sized(1, 1).eyeHeight(0).fireImmune());

    private static <E extends Entity> EntityType<E> register(String path, EntityType.Builder<E> builder) {
        final ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath("aeronautics", path));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.noLootTable().build(key));
    }
    public static void init() {}
    private AeroEntityTypes() {}
}
