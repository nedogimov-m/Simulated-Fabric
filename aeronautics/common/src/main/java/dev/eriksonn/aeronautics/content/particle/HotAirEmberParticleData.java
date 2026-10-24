package dev.eriksonn.aeronautics.content.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.eriksonn.aeronautics.index.AeroParticleTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class HotAirEmberParticleData implements ParticleOptions {

    public static final MapCodec<HotAirEmberParticleData> CODEC = RecordCodecBuilder.mapCodec((i) -> i.group(
                    Codec.BOOL.fieldOf("isSoul").forGetter((p) -> p.isSoul)
    ).apply(i, HotAirEmberParticleData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, HotAirEmberParticleData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, (p) -> p.isSoul,
            HotAirEmberParticleData::new);

    protected final boolean isSoul;

    public HotAirEmberParticleData(final boolean isSoul) {
        this.isSoul = isSoul;
    }

    public HotAirEmberParticleData() {
        this.isSoul = false;
    }

    @Override
    public ParticleType<?> getType() {
        return AeroParticleTypes.HOT_AIR_EMBER;
    }

}
