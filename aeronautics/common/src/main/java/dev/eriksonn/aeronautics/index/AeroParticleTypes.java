package dev.eriksonn.aeronautics.index;

import dev.eriksonn.aeronautics.content.particle.*;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/** Common particle payloads; providers are bound separately on the client. */
public final class AeroParticleTypes {
    public static final ParticleType<PropellerAirParticleData> PROPELLER_AIR_FLOW = Registry.register(
            BuiltInRegistries.PARTICLE_TYPE, Identifier.parse("aeronautics:propeller_air_flow"),
            FabricParticleTypes.complex(PropellerAirParticleData.CODEC, PropellerAirParticleData.STREAM_CODEC));
    public static final ParticleType<HotAirEmberParticleData> HOT_AIR_EMBER = Registry.register(
            BuiltInRegistries.PARTICLE_TYPE, Identifier.parse("aeronautics:hot_air_ember"),
            FabricParticleTypes.complex(HotAirEmberParticleData.CODEC, HotAirEmberParticleData.STREAM_CODEC));
    public static final ParticleType<LevititeSparkleParticleData> LEVITITE_SPARKLE = Registry.register(
            BuiltInRegistries.PARTICLE_TYPE, Identifier.parse("aeronautics:levitite_sparkle"),
            FabricParticleTypes.complex(LevititeSparkleParticleData.CODEC, LevititeSparkleParticleData.STREAM_CODEC));
    public static final ParticleType<GustParticleData> GUST = Registry.register(
            BuiltInRegistries.PARTICLE_TYPE, Identifier.parse("aeronautics:gust"),
            FabricParticleTypes.complex(GustParticleData.CODEC, GustParticleData.STREAM_CODEC));
    public static final ParticleType<AirPoofParticleData> AIR_POOF = Registry.register(
            BuiltInRegistries.PARTICLE_TYPE, Identifier.parse("aeronautics:air_poof"),
            FabricParticleTypes.complex(AirPoofParticleData.CODEC, AirPoofParticleData.STREAM_CODEC));

    public static void init() {}
    private AeroParticleTypes() {}
}
