package dev.simulated_team.simulated.fabric.registry;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/** One actual registered block per vanilla dye color, in vanilla color order. */
public final class NativeDyedBlocks<T extends Block> implements Iterable<T> {
    private final EnumMap<DyeColor, T> blocks = new EnumMap<>(DyeColor.class);

    public NativeDyedBlocks(Function<DyeColor, T> factory) {
        for (DyeColor color : DyeColor.values()) this.blocks.put(color, Objects.requireNonNull(factory.apply(color)));
    }

    public T get(DyeColor color) { return this.blocks.get(Objects.requireNonNull(color)); }
    public boolean contains(Block block) { return this.blocks.containsValue(block); }
    public List<T> values() { return List.copyOf(this.blocks.values()); }
    public Block[] toArray() { return this.blocks.values().toArray(Block[]::new); }
    public Block[] toArray(Block[] target) { return this.blocks.values().toArray(target); }
    @Override public Iterator<T> iterator() { return this.values().iterator(); }
}
