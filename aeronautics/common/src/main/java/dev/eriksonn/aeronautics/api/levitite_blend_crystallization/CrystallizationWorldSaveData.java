package dev.eriksonn.aeronautics.api.levitite_blend_crystallization;

import dev.ryanhcode.sable.sublevel.storage.SableSavedDataStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Per-world crystallization state, retaining the original NBT layout and legacy file. */
public final class CrystallizationWorldSaveData extends SavedData {
    public static final String ID = "aeronautics_levitite_data";
    public static final Identifier STORAGE_ID = Identifier.parse("aeronautics:crystallization");
    private final ServerLevel level;

    private CrystallizationWorldSaveData(final ServerLevel level) {
        this.level = level;
    }

    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        final ListTag list = new ListTag();
        LevititeCrystallizerManager.saveData(list, level);
        tag.put("Levitite Manager Data", list);
        return tag;
    }

    public static CrystallizationWorldSaveData load(final ServerLevel level, final CompoundTag tag) {
        final var data = new CrystallizationWorldSaveData(level);
        LevititeCrystallizerManager.loadData(tag, level);
        return data;
    }

    public static CrystallizationWorldSaveData get(final ServerLevel level) {
        final var storage = level.getDataStorage();
        // Sable exposes the native storage folder through its production access widener.
        return SableSavedDataStorage.getOrLoad(storage, storage.dataFolder, STORAGE_ID, ID,
                () -> new CrystallizationWorldSaveData(level), tag -> load(level, tag),
                CrystallizationWorldSaveData::save, null);
    }
}
