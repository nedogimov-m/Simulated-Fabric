package dev.eriksonn.aeronautics.content.particle;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.eriksonn.aeronautics.index.AeroParticleTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import org.joml.Quaternionf;

public record GustParticleData(
        Quaternionf orientation) implements ParticleOptions {

    public static final MapCodec<GustParticleData> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ExtraCodecs.QUATERNIONF.xmap(Quaternionf::new, value -> value).fieldOf("orientation").forGetter(o -> o.orientation)
    ).apply(instance, GustParticleData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, GustParticleData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.QUATERNIONF.map(Quaternionf::new, value -> value), (o -> o.orientation),
            GustParticleData::new
    );

    public GustParticleData() {
        this(new Quaternionf());
    }

    @Override
    public ParticleType<?> getType() {
        return AeroParticleTypes.GUST;
    }
}
