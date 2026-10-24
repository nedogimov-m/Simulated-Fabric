package dev.simulated_team.simulated.fabric.registry;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;

/** Real registry-aware item NBT and native block-entity loading for existing Ponder scene mutations. */
public final class NativePonderNbt {
    public static Tag item(HolderLookup.Provider lookup,ItemStack stack){return ItemStack.OPTIONAL_CODEC.encodeStart(lookup.createSerializationContext(NbtOps.INSTANCE),stack).getOrThrow();}
    public static void load(BlockEntity entity,CompoundTag tag,HolderLookup.Provider lookup){entity.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,lookup,tag));}
    private NativePonderNbt() { }
}
