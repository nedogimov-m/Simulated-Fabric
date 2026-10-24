package dev.simulated_team.simulated.content.blocks.behaviour;

import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotationIndicatorParticleData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** Sim-owned native particle emission; Create26.3 has no global KineticEffectHandler field. */
public class SimKineticEffectHandler {
    protected final KineticBlockEntity owner;
    public SimKineticEffectHandler(final KineticBlockEntity owner) { this.owner = owner; }
    public void triggerOverStressedEffect() {
        final var level = this.owner.getLevel();
        if (level == null) return;
        final var center = Vec3.atCenterOf(this.owner.getBlockPos());
        if (level instanceof final ServerLevel server) server.sendParticles(ParticleTypes.SMOKE, center.x, center.y, center.z, 8, .3, .3, .3, .01);
        else for (int i = 0; i < 8; i++) level.addParticle(ParticleTypes.SMOKE, center.x + (level.getRandom().nextDouble() - .5) * .6,
                center.y + (level.getRandom().nextDouble() - .5) * .6, center.z + (level.getRandom().nextDouble() - .5) * .6, 0, .02, 0);
    }
    public void spawnRotationIndicators() {
        final var level = this.owner.getLevel(); final float speed = this.owner.getSpeed();
        if (level == null || speed == 0 || !(this.owner.getBlockState().getBlock() instanceof final IRotate rotation)) return;
        final var tier = IRotate.SpeedLevel.of(speed); final var center = Vec3.atCenterOf(this.owner.getBlockPos());
        final var particles = new RotationIndicatorParticleData(tier.getColor(), tier.getParticleSpeed() * Math.signum(speed), .75f, 1.125f, 20,
                rotation.getRotationAxis(this.owner.getBlockState()));
        if (level instanceof final ServerLevel server) server.sendParticles(particles, center.x, center.y, center.z, 20, 0, 0, 0, 1);
        else for (int i = 0; i < 20; i++) level.addParticle(particles, center.x, center.y, center.z, 0, 0, 0);
    }
}
