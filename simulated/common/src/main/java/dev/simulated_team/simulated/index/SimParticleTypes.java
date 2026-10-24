package dev.simulated_team.simulated.index;

import dev.simulated_team.simulated.content.particle.AugerIndicatorParticleData;
import dev.simulated_team.simulated.content.particle.MagnetFieldParticleData;
import dev.simulated_team.simulated.content.particle.MagnetFieldParticleData2;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/** Common particle payloads; providers are bound separately on the client. */
public final class SimParticleTypes {
    public static final ParticleType<MagnetFieldParticleData> MAGNET_FIELD = Registry.register(
            BuiltInRegistries.PARTICLE_TYPE, Identifier.parse("simulated:magnet_field"),
            FabricParticleTypes.complex(MagnetFieldParticleData.CODEC, MagnetFieldParticleData.STREAM_CODEC));
    public static final ParticleType<MagnetFieldParticleData2> MAGNET_FIELD2 = Registry.register(
            BuiltInRegistries.PARTICLE_TYPE, Identifier.parse("simulated:magnet_field2"),
            FabricParticleTypes.complex(MagnetFieldParticleData2.CODEC, MagnetFieldParticleData2.STREAM_CODEC));
    public static final ParticleType<AugerIndicatorParticleData> AUGER_INDICATOR = Registry.register(
            BuiltInRegistries.PARTICLE_TYPE, Identifier.parse("simulated:auger_indicator"),
            FabricParticleTypes.complex(AugerIndicatorParticleData.CODEC, AugerIndicatorParticleData.STREAM_CODEC));

    public static void register() {}
    private SimParticleTypes() {}
}
