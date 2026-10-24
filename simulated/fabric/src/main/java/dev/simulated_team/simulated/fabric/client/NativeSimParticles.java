package dev.simulated_team.simulated.fabric.client;

import dev.simulated_team.simulated.index.SimParticleTypes;
import dev.simulated_team.simulated.content.particle.*;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;

/** Sprite providers are registered only from the client initializer. */
public final class NativeSimParticles {
    public static void register() {
        final ParticleProviderRegistry providers = ParticleProviderRegistry.getInstance();
        providers.register(SimParticleTypes.MAGNET_FIELD, MagnetFieldParticle.Factory::new);
        providers.register(SimParticleTypes.MAGNET_FIELD2, MagnetFieldParticle2.Factory::new);
        providers.register(SimParticleTypes.AUGER_INDICATOR, AugerIndicatorParticle.Factory::new);
    }
    private NativeSimParticles() {}
}
