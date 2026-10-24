package dev.simulated_team.simulated.fabric.registry;

import dev.simulated_team.simulated.content.blocks.auger_shaft.AugerShaftRenderer;
import dev.simulated_team.simulated.content.blocks.altitude_sensor.AltitudeSensorRenderer;
import dev.simulated_team.simulated.content.blocks.analog_transmission.AnalogTransmissionRenderer;
import dev.simulated_team.simulated.content.blocks.directional_gearshift.DirectionalGearshiftRenderer;
import dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorRenderer;
import dev.simulated_team.simulated.content.blocks.gimbal_sensor.GimbalSensorRenderer;
import dev.simulated_team.simulated.content.blocks.lasers.laser_pointer.LaserPointerRenderer;
import dev.simulated_team.simulated.content.blocks.lasers.optical_sensor.OpticalSensorRenderer;
import dev.simulated_team.simulated.content.blocks.merging_glue.MergingGlueRenderer;
import dev.simulated_team.simulated.content.blocks.nameplate.NameplateRenderer;
import dev.simulated_team.simulated.content.blocks.nav_table.NavTableRenderer;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerRenderer;
import dev.simulated_team.simulated.content.blocks.portable_engine.PortableEngineRenderer;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterRenderer;
import dev.simulated_team.simulated.content.blocks.redstone.modulating_receiver.ModulatingLinkedReceiverRenderer;
import dev.simulated_team.simulated.content.blocks.redstone.redstone_accumulator.RedstoneAccumulatorRenderer;
import dev.simulated_team.simulated.content.blocks.redstone.redstone_inductor.RedstoneInductorRenderer;
import dev.simulated_team.simulated.content.blocks.rope.rope_connector.RopeConnectorRenderer;
import dev.simulated_team.simulated.content.blocks.rope.rope_winch.RopeWinchRenderer;
import dev.simulated_team.simulated.content.blocks.spring.SpringRenderer;
import dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelRenderer;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingRenderer;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.link_block.SwivelBearingPlateBlockRenderer;
import dev.simulated_team.simulated.content.blocks.throttle_lever.ThrottleLeverRenderer;
import dev.simulated_team.simulated.content.blocks.torsion_spring.TorsionSpringRenderer;
import dev.simulated_team.simulated.content.blocks.velocity_sensor.VelocitySensorRenderer;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;

/**
 * Explicit client registrations for every original BER. Former Flywheel visual-only paths must be
 * migrated into each renderer's native render-state submission; no instancing API is loaded on servers.
 * The original non-instanced animation implementations remain in their renderer source files.
 */
public final class NativeBlockEntityRenderers {
    private static boolean registered;

    public static void register() {
        if (registered) return;
        BlockEntityRendererRegistry.register(SimBlockEntityTypes.SIMPLE_BE, AnalogTransmissionRenderer::new);
        BlockEntityRendererRegistry.register(SimBlockEntityTypes.TORSION_SPRING, TorsionSpringRenderer::new);
        BlockEntityRendererRegistry.register(SimBlockEntityTypes.PHYSICS_ASSEMBLER, PhysicsAssemblerRenderer::new);
        BlockEntityRendererRegistry.register(SimBlockEntityTypes.ROPE_WINCH, RopeWinchRenderer::new);
        BlockEntityRendererRegistry.register(SimBlockEntityTypes.ROPE_CONNECTOR, RopeConnectorRenderer::new);
        BlockEntityRendererRegistry.register(SimBlockEntityTypes.DOCKING_CONNECTOR, DockingConnectorRenderer::new);
        BlockEntityRendererRegistry.register(SimBlockEntityTypes.ALTITUDE_SENSOR, AltitudeSensorRenderer::new);
        BlockEntityRendererRegistry.register(SimBlockEntityTypes.GIMBAL_SENSOR, GimbalSensorRenderer::new);
        BlockEntityRendererRegistry.register(SimBlockEntityTypes.PORTABLE_ENGINE, PortableEngineRenderer::new);
        BlockEntityRendererRegistry.register(SimBlockEntityTypes.SWIVEL_BEARING, SwivelBearingRenderer::new);
        BlockEntityRendererRegistry.register(SimBlockEntityTypes.STEERING_WHEEL, SteeringWheelRenderer::new);
        BlockEntityRendererRegistry.register(SimBlockEntityTypes.SPRING, SpringRenderer::new);
        BlockEntityRendererRegistry.register(SimBlockEntityTypes.SWIVEL_BEARING_LINK_BLOCK, SwivelBearingPlateBlockRenderer::new);
        BlockEntityRendererRegistry.register(SimBlockEntityTypes.LINKED_TYPEWRITER, LinkedTypewriterRenderer::new);
        BlockEntityRendererRegistry.register(SimBlockEntityTypes.OPTICAL_SENSOR, OpticalSensorRenderer::new);
        BlockEntityRendererRegistry.register(SimBlockEntityTypes.LASER_POINTER, LaserPointerRenderer::new);
        // LASER_SENSOR has no dynamic geometry; Create's native global behaviour renderer owns its filter/value boxes.
        BlockEntityRendererRegistry.register(SimBlockEntityTypes.VELOCITY_SENSOR, VelocitySensorRenderer::new);
        // DIRECTIONAL_LINKED_RECEIVER likewise used only the now-global native behaviour renderer.
        BlockEntityRendererRegistry.register(SimBlockEntityTypes.NAMEPLATE, NameplateRenderer::new);
        BlockEntityRendererRegistry.register(SimBlockEntityTypes.MODULATING_LINKED_RECEIVER, ModulatingLinkedReceiverRenderer::new);
        BlockEntityRendererRegistry.register(SimBlockEntityTypes.REDSTONE_ACCUMULATOR, RedstoneAccumulatorRenderer::new);
        BlockEntityRendererRegistry.register(SimBlockEntityTypes.NAVIGATION_TABLE, NavTableRenderer::new);
        BlockEntityRendererRegistry.register(SimBlockEntityTypes.REDSTONE_INDUCTOR, RedstoneInductorRenderer::new);
        BlockEntityRendererRegistry.register(SimBlockEntityTypes.THROTTLE_LEVER, ThrottleLeverRenderer::new);
        BlockEntityRendererRegistry.register(SimBlockEntityTypes.AUGER_SHAFT, AugerShaftRenderer::new);
        BlockEntityRendererRegistry.register(SimBlockEntityTypes.MERGING_GLUE, MergingGlueRenderer::new);
        BlockEntityRendererRegistry.register(SimBlockEntityTypes.DIRECTIONAL_GEARSHIFT, DirectionalGearshiftRenderer::new);
        com.simibubi.create.content.contraptions.render.ActorRenderer.REGISTRY.register(
                dev.simulated_team.simulated.index.SimBlocks.ALTITUDE_SENSOR,
                new dev.simulated_team.simulated.content.blocks.altitude_sensor.AltitudeSensorActorRenderer());
        registered = true;
    }

    private NativeBlockEntityRenderers() {}
}
