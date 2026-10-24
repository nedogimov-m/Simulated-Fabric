package dev.simulated_team.simulated.fabric.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/** Explicit native creative ordering, section ownership and client presentation metadata. */
public final class NativeItemCatalog {
    public enum Visibility { MAIN, SEARCH_ONLY, INVISIBLE }
    public record Entry(Item item, Identifier section, Visibility visibility) {}
    private static final Map<Identifier, Entry> ITEMS = new LinkedHashMap<>();
    private static final Map<Identifier, Block> BLOCKS = new LinkedHashMap<>();
    private static final Set<Block> CUTOUT = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Map<Item, String> TOOLTIP_KEYS = new IdentityHashMap<>();

    public static synchronized void add(Item item, Identifier section, Visibility visibility) {
        final Identifier id = BuiltInRegistries.ITEM.getKey(item);
        final Entry entry = new Entry(Objects.requireNonNull(item), Objects.requireNonNull(section), Objects.requireNonNull(visibility));
        if (ITEMS.putIfAbsent(id, entry) != null) throw new IllegalStateException("Duplicate native creative item " + id);
    }

    public static synchronized void addExtra(Identifier id, Identifier section) {
        if (!BuiltInRegistries.ITEM.containsKey(id)) throw new IllegalArgumentException("Unknown extra creative item " + id);
        add(BuiltInRegistries.ITEM.getValue(id), section, Visibility.MAIN);
    }

    public static synchronized void block(Block block, boolean cutout) {
        final Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        if (BLOCKS.putIfAbsent(id, block) != null) throw new IllegalStateException("Duplicate native block metadata " + id);
        if (cutout) CUTOUT.add(block);
    }

    public static synchronized void tooltipKey(Item item, String key) { TOOLTIP_KEYS.put(item, key); }
    public static synchronized List<Entry> entries() { return List.copyOf(ITEMS.values()); }
    public static synchronized Map<Identifier, Block> blocks() { return Collections.unmodifiableMap(new LinkedHashMap<>(BLOCKS)); }
    public static synchronized Map<Item, String> tooltipKeys() { return Map.copyOf(TOOLTIP_KEYS); }
    public static synchronized boolean isCutout(Block block) { return CUTOUT.contains(block); }
    public static synchronized Identifier sectionOf(Item item) {
        var entry = ITEMS.get(BuiltInRegistries.ITEM.getKey(item));
        return entry == null ? null : entry.section();
    }

    /** The section UI can group these stable entries without changing source visibility rules. */
    public static void processItems(Identifier section, Consumer<ItemStack> display, Consumer<ItemStack> search) {
        for (Entry entry : entries()) {
            if (!entry.section().equals(section) || entry.visibility() == Visibility.INVISIBLE) continue;
            final ItemStack stack = entry.item().getDefaultInstance();
            if (entry.visibility() == Visibility.MAIN) display.accept(stack.copy());
            search.accept(stack);
        }
    }

    private NativeItemCatalog() {}
}
