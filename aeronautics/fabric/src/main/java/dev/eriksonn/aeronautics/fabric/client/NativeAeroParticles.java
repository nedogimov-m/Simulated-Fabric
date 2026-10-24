package dev.eriksonn.aeronautics.fabric.client;

import dev.eriksonn.aeronautics.index.AeroParticleTypes;
import dev.eriksonn.aeronautics.content.particle.*;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;

/** Sprite providers are registered only from the client initializer. */
public final class NativeAeroParticles {
    public static void register() {
        final ParticleProviderRegistry providers = ParticleProviderRegistry.getInstance();
        providers.register(AeroParticleTypes.PROPELLER_AIR_FLOW, PropellerAirParticle.Factory::new);
        providers.register(AeroParticleTypes.HOT_AIR_EMBER, HotAirEmberParticle.Factory::new);
        providers.register(AeroParticleTypes.LEVITITE_SPARKLE, LevititeSparkleParticle.Factory::new);
        providers.register(AeroParticleTypes.GUST, GustParticle.Factory::new);
        providers.register(AeroParticleTypes.AIR_POOF, AirPoofParticle.Factory::new);
    }
    private NativeAeroParticles() {}
}
