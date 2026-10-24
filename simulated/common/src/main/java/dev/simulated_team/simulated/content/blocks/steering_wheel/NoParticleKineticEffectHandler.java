package dev.simulated_team.simulated.content.blocks.steering_wheel;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.simulated_team.simulated.content.blocks.behaviour.SimKineticEffectHandler;

public class NoParticleKineticEffectHandler extends SimKineticEffectHandler {
    public NoParticleKineticEffectHandler(final KineticBlockEntity kte) {
        super(kte);
    }

    @Override
    public void spawnRotationIndicators() {
    }
}
