package dev.ryanhcode.offroad.index;

import dev.ryanhcode.offroad.content.components.TireLike;
import dev.ryanhcode.offroad.content.items.tire.TireItem;
import dev.simulated_team.simulated.fabric.registry.NativeItemCatalog;
import dev.simulated_team.simulated.fabric.registry.NativeRegistrations;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/** Real native tire items; their component defaults are used by both placement and recipes. */
public final class OffroadItems {
    private static final Identifier SECTION = id("offroad");
    public static final TireItem SMALL_TIRE = tire("small_tire", TireLike.SMALL_TIRE);
    public static final TireItem TIRE = tire("tire", TireLike.TIRE);
    public static final TireItem LARGE_TIRE = tire("large_tire", TireLike.LARGE_TIRE);
    public static final TireItem MONSTROUS_TIRE = tire("monstrous_tire", TireLike.MONSTROUS_TIRE);

    private static TireItem tire(String name, TireLike tire) {
        final TireItem item = NativeRegistrations.item(id(name), TireItem::new,
                new Item.Properties().component(OffroadDataComponents.TIRE, tire));
        NativeItemCatalog.add(item, SECTION, NativeItemCatalog.Visibility.MAIN);
        return item;
    }

    public static void init() {}
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("offroad", path); }
    private OffroadItems() {}
}
