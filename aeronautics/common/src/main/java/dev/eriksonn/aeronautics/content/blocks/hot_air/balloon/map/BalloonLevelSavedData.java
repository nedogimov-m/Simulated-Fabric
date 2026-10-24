package dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.map;

import com.mojang.serialization.Codec;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.Balloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.ServerBalloon;
import dev.ryanhcode.sable.sublevel.storage.SableSavedDataStorage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.List;

/** Native saved-data storage retains the original file and list-of-balloon NBT format. */
public final class BalloonLevelSavedData extends SavedData {
    public static final String ID = "aeronautics_unloaded_balloons";
    public static final Identifier STORAGE_ID = Identifier.parse("aeronautics:unloaded_balloons");
    public static final Codec<List<SavedBalloon>> CODEC = SavedBalloon.CODEC.listOf();
    private final ServerLevel level;

    private BalloonLevelSavedData(ServerLevel level) { this.level = level; }
    private static BalloonLevelSavedData load(ServerLevel level, CompoundTag tag) {
        if (tag.contains(ID)) CODEC.parse(NbtOps.INSTANCE, tag.getListOrEmpty(ID))
                .ifSuccess(data -> BalloonMap.MAP.get(level).getUnloadedBalloons().addAll(data));
        return new BalloonLevelSavedData(level);
    }
    public static BalloonLevelSavedData get(ServerLevel level) {
        final var storage = level.getDataStorage();
        return SableSavedDataStorage.getOrLoad(storage, storage.dataFolder, STORAGE_ID, ID,
                () -> new BalloonLevelSavedData(level), tag -> load(level, tag), BalloonLevelSavedData::save, null);
    }
    public CompoundTag save() {
        final var map = BalloonMap.MAP.get(this.level);
        final var list = new ObjectArrayList<>(map.getUnloadedBalloons());
        for (final Balloon balloon : map.getBalloons()) list.add(BalloonMap.saveBalloon((ServerBalloon) balloon));
        final var tag = new CompoundTag();
        CODEC.encodeStart(NbtOps.INSTANCE, list).ifSuccess(data -> tag.put(ID, data));
        return tag;
    }
}
