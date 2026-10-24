package dev.eriksonn.aeronautics.content.particle;

import com.mojang.serialization.MapCodec;
import dev.eriksonn.aeronautics.index.AeroParticleTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class AirPoofParticleData implements ParticleOptions {
    public static final AirPoofParticleData INSTANCE = new AirPoofParticleData();
    public static final MapCodec<AirPoofParticleData> CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<FriendlyByteBuf, AirPoofParticleData> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    private AirPoofParticleData() {}

    @Override
    public ParticleType<?> getType() {
        return AeroParticleTypes.AIR_POOF;
    }
}
