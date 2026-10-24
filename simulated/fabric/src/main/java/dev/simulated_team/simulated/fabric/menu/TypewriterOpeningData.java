package dev.simulated_team.simulated.fabric.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Explicit opening payload preserves the original position followed by block-entity NBT. */
public record TypewriterOpeningData(BlockPos pos, CompoundTag tag) {
    public static final StreamCodec<RegistryFriendlyByteBuf, TypewriterOpeningData> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TypewriterOpeningData::pos,
            ByteBufCodecs.COMPOUND_TAG, TypewriterOpeningData::tag,
            TypewriterOpeningData::new);
    public TypewriterOpeningData {
        pos = pos.immutable();
        tag = tag.copy();
    }
}
