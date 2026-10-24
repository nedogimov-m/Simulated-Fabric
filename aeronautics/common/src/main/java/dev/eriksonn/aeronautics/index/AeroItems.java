package dev.eriksonn.aeronautics.index;

import dev.eriksonn.aeronautics.content.components.Levitating;
import dev.eriksonn.aeronautics.content.items.AviatorsGogglesItem;
import dev.simulated_team.simulated.fabric.registry.NativeItemCatalog;
import dev.simulated_team.simulated.fabric.registry.NativeRegistrations;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.function.Function;

/** Native items with the original item defaults and creative ordering. */
public final class AeroItems {
    private static final Identifier SECTION = id("aeronautics");
    public static final AviatorsGogglesItem AVIATORS_GOGGLES = item("aviators_goggles", AviatorsGogglesItem::new, new Item.Properties());
    public static final Item MUSIC_DISC_CLOUD_SKIPPER = item("music_disc_cloud_skipper", Item::new,
            new Item.Properties().stacksTo(1).rarity(Rarity.RARE)
                    .jukeboxPlayable(ResourceKey.create(Registries.JUKEBOX_SONG, id("cloud_skipper")))
                    .component(AeroDataComponents.LEVITATING, Levitating.DEFAULT));
    public static final Item ENDSTONE_POWDER = item("end_stone_powder", Item::new,
            new Item.Properties().component(AeroDataComponents.LEVITATING, Levitating.END_STONE));

    private static <I extends Item> I item(String name, Function<Item.Properties, I> factory, Item.Properties properties) {
        final I item = NativeRegistrations.item(id(name), factory, properties);
        NativeItemCatalog.add(item, SECTION, NativeItemCatalog.Visibility.MAIN);
        return item;
    }
    public static void init() {}
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("aeronautics", path); }
    private AeroItems() {}
}
