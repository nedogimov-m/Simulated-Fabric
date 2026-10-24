package dev.simulated_team.simulated.fabric.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;

/** Native registry construction with identifiers assigned before constructors consume their properties. */
public final class NativeRegistrations {
    private NativeRegistrations() {}

    public static <B extends Block> B block(Identifier id, Function<BlockBehaviour.Properties, B> factory,
                                            BlockBehaviour.Properties properties) {
        final ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
    }

    public static <I extends Item> I item(Identifier id, Function<Item.Properties, I> factory, Item.Properties properties) {
        final ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        final I item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
        if (item instanceof BlockItem blockItem) blockItem.registerBlocks(Item.BY_BLOCK, item);
        return item;
    }

    public static <E extends Entity> EntityType<E> entity(Identifier id, EntityType.EntityFactory<E> factory,
                                                         MobCategory category, EntitySettings settings) {
        final ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
        final EntityType.Builder<E> builder = EntityType.Builder.of(factory, category)
                .sized(settings.width(), settings.height())
                .clientTrackingRange(settings.clientTrackingRange())
                .updateInterval(settings.updateFrequency())
                .noLootTable();
        if (settings.immuneToFire()) builder.fireImmune();
        if (!settings.sendVelocity()) builder.dontTrackDeltas();
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }

    public record EntitySettings(int clientTrackingRange, int updateFrequency, float width, float height,
                                 boolean sendVelocity, boolean immuneToFire) {
        public EntitySettings {
            if (clientTrackingRange < 0 || updateFrequency < 1 || !Float.isFinite(width) || !Float.isFinite(height)
                    || width <= 0 || height <= 0) {
                throw new IllegalArgumentException("Invalid entity dimensions or tracking settings");
            }
        }
    }
}
