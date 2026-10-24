package dev.simulated_team.simulated.index;

import dev.simulated_team.simulated.content.blocks.altitude_sensor.AltitudeSensorBlockEntity;
import dev.simulated_team.simulated.content.blocks.analog_transmission.AnalogTransmissionBlockEntity;
import dev.simulated_team.simulated.content.blocks.auger_shaft.AugerShaftBlock;
import dev.simulated_team.simulated.content.blocks.auger_shaft.AugerShaftBlockEntity;
import dev.simulated_team.simulated.content.blocks.directional_gearshift.DirectionalGearshiftBlockEntity;
import dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorBlockEntity;
import dev.simulated_team.simulated.content.blocks.gimbal_sensor.GimbalSensorBlockEntity;
import dev.simulated_team.simulated.content.blocks.handle.HandleBlockEntity;
import dev.simulated_team.simulated.content.blocks.lasers.laser_pointer.LaserPointerBlockEntity;
import dev.simulated_team.simulated.content.blocks.lasers.laser_sensor.LaserSensorBlockEntity;
import dev.simulated_team.simulated.content.blocks.lasers.optical_sensor.OpticalSensorBlockEntity;
import dev.simulated_team.simulated.content.blocks.merging_glue.MergingGlueBlockEntity;
import dev.simulated_team.simulated.content.blocks.nameplate.NameplateBlockEntity;
import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlockEntity;
import dev.simulated_team.simulated.content.blocks.portable_engine.PortableEngineBlockEntity;
import dev.simulated_team.simulated.content.blocks.redstone.directional_receiver.DirectionalLinkedReceiverBlockEntity;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterBlockEntity;
import dev.simulated_team.simulated.content.blocks.redstone.modulating_receiver.ModulatingLinkedReceiverBlockEntity;
import dev.simulated_team.simulated.content.blocks.redstone.redstone_accumulator.RedstoneAccumulatorBlockEntity;
import dev.simulated_team.simulated.content.blocks.redstone.redstone_inductor.RedstoneInductorBlockEntity;
import dev.simulated_team.simulated.content.blocks.redstone_magnet.RedstoneMagnetBlockEntity;
import dev.simulated_team.simulated.content.blocks.rope.rope_connector.RopeConnectorBlockEntity;
import dev.simulated_team.simulated.content.blocks.rope.rope_winch.RopeWinchBlockEntity;
import dev.simulated_team.simulated.content.blocks.spring.SpringBlockEntity;
import dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelBlockEntity;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingBlockEntity;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.link_block.SwivelBearingPlateBlockEntity;
import dev.simulated_team.simulated.content.blocks.throttle_lever.ThrottleLeverBlockEntity;
import dev.simulated_team.simulated.content.blocks.torsion_spring.TorsionSpringBlockEntity;
import dev.simulated_team.simulated.content.blocks.velocity_sensor.VelocitySensorBlockEntity;
import dev.simulated_team.simulated.fabric.registry.NativeBlockEntities;
import dev.simulated_team.simulated.service.SimInventoryService;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Actual common BE registrations, independent of client renderer and instancing classes. */
public final class SimBlockEntityTypes {
    private static boolean registered;

    public static final BlockEntityType<AnalogTransmissionBlockEntity> SIMPLE_BE = NativeBlockEntities.register(
            id("simple"), AnalogTransmissionBlockEntity::new, SimBlocks.ANALOG_TRANSMISSION);

    public static final BlockEntityType<TorsionSpringBlockEntity> TORSION_SPRING = NativeBlockEntities.register(
            id("torsion_spring"), TorsionSpringBlockEntity::new, SimBlocks.TORSION_SPRING);

    public static final BlockEntityType<PhysicsAssemblerBlockEntity> PHYSICS_ASSEMBLER = NativeBlockEntities.register(
            id("physics_assembler"), PhysicsAssemblerBlockEntity::new, SimBlocks.PHYSICS_ASSEMBLER);

    public static final BlockEntityType<RopeWinchBlockEntity> ROPE_WINCH = NativeBlockEntities.register(
            id("rope_winch"), RopeWinchBlockEntity::new, SimBlocks.ROPE_WINCH);

    public static final BlockEntityType<RopeConnectorBlockEntity> ROPE_CONNECTOR = NativeBlockEntities.register(
            id("rope_connector"), RopeConnectorBlockEntity::new, SimBlocks.ROPE_CONNECTOR);

    public static final BlockEntityType<DockingConnectorBlockEntity> DOCKING_CONNECTOR = NativeBlockEntities.register(
            id("docking_connector"), DockingConnectorBlockEntity::new, SimBlocks.DOCKING_CONNECTOR);

    public static final BlockEntityType<AltitudeSensorBlockEntity> ALTITUDE_SENSOR = NativeBlockEntities.register(
            id("altitude_sensor"), AltitudeSensorBlockEntity::new, SimBlocks.ALTITUDE_SENSOR);

    public static final BlockEntityType<GimbalSensorBlockEntity> GIMBAL_SENSOR = NativeBlockEntities.register(
            id("gimbal_sensor"), GimbalSensorBlockEntity::new, SimBlocks.GIMBAL_SENSOR);

    public static final BlockEntityType<HandleBlockEntity> HANDLE = NativeBlockEntities.register(
            id("handle"), HandleBlockEntity::new, NativeBlockEntities.blocks(new net.minecraft.world.level.block.Block[]{SimBlocks.IRON_HANDLE, SimBlocks.COPPER_HANDLE}, SimBlocks.DYED_HANDLES.toArray()));

    public static final BlockEntityType<PortableEngineBlockEntity> PORTABLE_ENGINE = NativeBlockEntities.register(
            id("portable_engine"), PortableEngineBlockEntity::new, SimBlocks.PORTABLE_ENGINES.toArray());

    public static final BlockEntityType<SwivelBearingBlockEntity> SWIVEL_BEARING = NativeBlockEntities.register(
            id("swivel_bearing"), SwivelBearingBlockEntity::new, SimBlocks.SWIVEL_BEARING);

    public static final BlockEntityType<SteeringWheelBlockEntity> STEERING_WHEEL = NativeBlockEntities.register(
            id("steering_wheel"), SteeringWheelBlockEntity::new, SimBlocks.STEERING_WHEEL);

    public static final BlockEntityType<SpringBlockEntity> SPRING = NativeBlockEntities.register(
            id("spring"), SpringBlockEntity::new, SimBlocks.SPRING);

    public static final BlockEntityType<SwivelBearingPlateBlockEntity> SWIVEL_BEARING_LINK_BLOCK = NativeBlockEntities.register(
            id("swivel_bearing_link_block"), SwivelBearingPlateBlockEntity::new, SimBlocks.SWIVEL_BEARING_LINK_BLOCK);

    public static final BlockEntityType<LinkedTypewriterBlockEntity> LINKED_TYPEWRITER = NativeBlockEntities.register(
            id("linked_typewriter"), LinkedTypewriterBlockEntity::new, SimBlocks.LINKED_TYPEWRITER);

    public static final BlockEntityType<OpticalSensorBlockEntity> OPTICAL_SENSOR = NativeBlockEntities.register(
            id("optical_sensor"), OpticalSensorBlockEntity::new, SimBlocks.OPTICAL_SENSOR);

    public static final BlockEntityType<LaserPointerBlockEntity> LASER_POINTER = NativeBlockEntities.register(
            id("laser_pointer"), LaserPointerBlockEntity::new, SimBlocks.LASER_POINTER);

    public static final BlockEntityType<LaserSensorBlockEntity> LASER_SENSOR = NativeBlockEntities.register(
            id("ir_sensor"), LaserSensorBlockEntity::new, SimBlocks.LASER_SENSOR);

    public static final BlockEntityType<VelocitySensorBlockEntity> VELOCITY_SENSOR = NativeBlockEntities.register(
            id("velocity_sensor"), VelocitySensorBlockEntity::new, SimBlocks.VELOCITY_SENSOR);

    public static final BlockEntityType<DirectionalLinkedReceiverBlockEntity> DIRECTIONAL_LINKED_RECEIVER = NativeBlockEntities.register(
            id("directional_linked_receiver"), DirectionalLinkedReceiverBlockEntity::new, SimBlocks.DIRECTIONAL_LINKED_RECEIVER);

    public static final BlockEntityType<NameplateBlockEntity> NAMEPLATE = NativeBlockEntities.register(
            id("nameplate"), NameplateBlockEntity::new, SimBlocks.NAMEPLATES.toArray());

    public static final BlockEntityType<ModulatingLinkedReceiverBlockEntity> MODULATING_LINKED_RECEIVER = NativeBlockEntities.register(
            id("modulating_linked_receiver"), ModulatingLinkedReceiverBlockEntity::new, SimBlocks.MODULATING_LINKED_RECEIVER);

    public static final BlockEntityType<RedstoneAccumulatorBlockEntity> REDSTONE_ACCUMULATOR = NativeBlockEntities.register(
            id("redstone_accumulator"), RedstoneAccumulatorBlockEntity::new, SimBlocks.REDSTONE_ACCUMULATOR);

    public static final BlockEntityType<NavTableBlockEntity> NAVIGATION_TABLE = NativeBlockEntities.register(
            id("navigation_table"), NavTableBlockEntity::new, SimBlocks.NAVIGATION_TABLE);

    public static final BlockEntityType<RedstoneInductorBlockEntity> REDSTONE_INDUCTOR = NativeBlockEntities.register(
            id("redstone_inductor"), RedstoneInductorBlockEntity::new, SimBlocks.REDSTONE_INDUCTOR);

    public static final BlockEntityType<RedstoneMagnetBlockEntity> REDSTONE_MAGNET = NativeBlockEntities.register(
            id("redstone_magnet"), RedstoneMagnetBlockEntity::new, SimBlocks.REDSTONE_MAGNET);

    public static final BlockEntityType<ThrottleLeverBlockEntity> THROTTLE_LEVER = NativeBlockEntities.register(
            id("throttle_lever"), ThrottleLeverBlockEntity::new, SimBlocks.THROTTLE_LEVER);

    public static final BlockEntityType<AugerShaftBlockEntity> AUGER_SHAFT = NativeBlockEntities.register(
            id("auger_shaft"), AugerShaftBlockEntity::new, SimBlocks.AUGER_SHAFT, SimBlocks.AUGER_COG);

    public static final BlockEntityType<MergingGlueBlockEntity> MERGING_GLUE = NativeBlockEntities.register(
            id("merging_glue"), MergingGlueBlockEntity::new, SimBlocks.MERGING_GLUE);

    public static final BlockEntityType<DirectionalGearshiftBlockEntity> DIRECTIONAL_GEARSHIFT = NativeBlockEntities.register(
            id("directional_gearshift"), DirectionalGearshiftBlockEntity::new, SimBlocks.DIRECTIONAL_GEARSHIFT);

    public static void register() {
        if (registered) return;
        final SimInventoryService storage = SimInventoryService.INSTANCE;
        storage.<DockingConnectorBlockEntity>registerInventory((be, side) -> be.getInventory()).accept(DOCKING_CONNECTOR);
        storage.<DockingConnectorBlockEntity>registerTank((be, side) -> be.tank).accept(DOCKING_CONNECTOR);
        storage.<DockingConnectorBlockEntity>registerBattery((be, side) -> be.battery).accept(DOCKING_CONNECTOR);
        storage.<PortableEngineBlockEntity>registerInventory((be, side) -> be.inventory).accept(PORTABLE_ENGINE);
        storage.<NavTableBlockEntity>registerInventory((be, side) -> be.inventory).accept(NAVIGATION_TABLE);
        storage.<AugerShaftBlockEntity>registerInventory((be, side) -> {
            if (side == null || be.getBlockState().is(SimBlocks.AUGER_COG)) return null;
            return side.getAxis() == be.getBlockState().getValue(AugerShaftBlock.AXIS) ? null : be.getInventory();
        }).accept(AUGER_SHAFT);
        registered = true;
    }

    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("simulated", path); }
    private SimBlockEntityTypes() {}
}
