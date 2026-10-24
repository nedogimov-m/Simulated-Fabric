package dev.simulated_team.simulated.index;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.behaviour.display.DisplayTarget;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.contraptions.wrench.RadialWrenchRotation;
import dev.simulated_team.simulated.content.blocks.altitude_sensor.AltitudeSensorBlock;
import dev.simulated_team.simulated.content.blocks.altitude_sensor.AltitudeSensorMovementBehaviour;
import dev.simulated_team.simulated.content.blocks.analog_transmission.AnalogTransmissionBlock;
import dev.simulated_team.simulated.content.blocks.auger_shaft.AugerCogBlock;
import dev.simulated_team.simulated.content.blocks.auger_shaft.AugerShaftBlock;
import dev.simulated_team.simulated.content.blocks.directional_gearshift.DirectionalGearshiftBlock;
import dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorBlock;
import dev.simulated_team.simulated.content.blocks.docking_connector.PairedDockingConnectorBlock;
import dev.simulated_team.simulated.content.blocks.gimbal_sensor.GimbalSensorBlock;
import dev.simulated_team.simulated.content.blocks.handle.HandleBlock;
import dev.simulated_team.simulated.content.blocks.lasers.laser_pointer.LaserPointerBlock;
import dev.simulated_team.simulated.content.blocks.lasers.laser_sensor.LaserSensorBlock;
import dev.simulated_team.simulated.content.blocks.lasers.optical_sensor.OpticalSensorBlock;
import dev.simulated_team.simulated.content.blocks.merging_glue.MergingGlueBlock;
import dev.simulated_team.simulated.content.blocks.nameplate.NameplateBlock;
import dev.simulated_team.simulated.content.blocks.nameplate.NameplateBlockTarget;
import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlock;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlock;
import dev.simulated_team.simulated.content.blocks.portable_engine.PortableEngineBlock;
import dev.simulated_team.simulated.content.blocks.redstone.directional_receiver.DirectionalLinkedReceiverBlock;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterBlock;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterItem;
import dev.simulated_team.simulated.content.blocks.redstone.modulating_receiver.ModulatingLinkedReceiverBlock;
import dev.simulated_team.simulated.content.blocks.redstone.redstone_accumulator.RedstoneAccumulatorBlock;
import dev.simulated_team.simulated.content.blocks.redstone.redstone_inductor.RedstoneInductorBlock;
import dev.simulated_team.simulated.content.blocks.redstone_magnet.RedstoneMagnetBlock;
import dev.simulated_team.simulated.content.blocks.rope.rope_connector.RopeConnectorBlock;
import dev.simulated_team.simulated.content.blocks.rope.rope_winch.RopeWinchBlock;
import dev.simulated_team.simulated.content.blocks.spring.SpringBlock;
import dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelBlock;
import dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelBlockEntity;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingBlock;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.link_block.SwivelBearingPlateBlock;
import dev.simulated_team.simulated.content.blocks.symmetric_sail.SymmetricSailBlock;
import dev.simulated_team.simulated.content.blocks.throttle_lever.ThrottleLeverBlock;
import dev.simulated_team.simulated.content.blocks.torsion_spring.TorsionSpringBlock;
import dev.simulated_team.simulated.content.blocks.velocity_sensor.VelocitySensorBlock;
import dev.simulated_team.simulated.fabric.registry.NativeDyedBlocks;
import dev.simulated_team.simulated.fabric.registry.NativeItemCatalog;
import dev.simulated_team.simulated.fabric.registry.NativeItemCatalog.Visibility;
import dev.simulated_team.simulated.fabric.registry.NativeItemUseOverrides;
import dev.simulated_team.simulated.fabric.registry.NativeRegistrations;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.function.BiFunction;
import java.util.function.Function;

/** Native runtime registrations. Models, tags, loot and recipes are shipped generated resources. */
public final class SimBlocks {
    private static final Identifier SECTION = id("simulated");
    private static boolean registered;

    public static final PhysicsAssemblerBlock PHYSICS_ASSEMBLER = withItem("physics_assembler", PhysicsAssemblerBlock::new,
            wooden(), false);

    public static final SwivelBearingBlock SWIVEL_BEARING = withItem("swivel_bearing", SwivelBearingBlock::new,
            netheriteMetal().destroyTime(5f).noOcclusion(), true);

    public static final SwivelBearingPlateBlock SWIVEL_BEARING_LINK_BLOCK = withoutItem("swivel_bearing_link_block", SwivelBearingPlateBlock::new,
            netheriteMetal().destroyTime(5f), false);

    public static final MergingGlueBlock MERGING_GLUE = withoutItem("merging_glue", MergingGlueBlock::new,
            BlockBehaviour.Properties.of().noOcclusion().instabreak().mapColor(MapColor.GRASS)
                    .friction(0.8f).sound(SoundType.SLIME_BLOCK), true);

    public static final RopeWinchBlock ROPE_WINCH = withItem("rope_winch", RopeWinchBlock::new,
            stone().noOcclusion(), true);

    public static final RopeConnectorBlock ROPE_CONNECTOR = withItem("rope_connector", RopeConnectorBlock::new,
            stone().noOcclusion().forceSolidOn(), true);

    public static final HandleBlock IRON_HANDLE = createHandle(null, HandleBlock.Variant.IRON);
    public static final HandleBlock COPPER_HANDLE = createHandle(null, HandleBlock.Variant.COPPER);
    public static final NativeDyedBlocks<HandleBlock> DYED_HANDLES =
            new NativeDyedBlocks<>(color -> createHandle(color, HandleBlock.Variant.DYED));

    public static final DirectionalGearshiftBlock DIRECTIONAL_GEARSHIFT = withItem("directional_gearshift", DirectionalGearshiftBlock::new,
            stone().noOcclusion().isRedstoneConductor(SimBlocks::never).mapColor(MapColor.PODZOL), true);

    public static final TorsionSpringBlock TORSION_SPRING = withItem("torsion_spring", TorsionSpringBlock::new,
            stone().noOcclusion(), true);

    public static final AugerShaftBlock AUGER_SHAFT = withItem("auger_shaft", AugerShaftBlock::new,
            softMetal().sound(SoundType.NETHERITE_BLOCK).noOcclusion(), true);

    public static final AugerCogBlock AUGER_COG = withItem("auger_cog", AugerCogBlock::new,
            softMetal().sound(SoundType.NETHERITE_BLOCK).noOcclusion(), true);

    public static final NativeDyedBlocks<PortableEngineBlock> PORTABLE_ENGINES =
            new NativeDyedBlocks<>(SimBlocks::createPortableEngine);
    public static final PortableEngineBlock RED_PORTABLE_ENGINE = PORTABLE_ENGINES.get(DyeColor.RED);

    public static final LaserPointerBlock LASER_POINTER = withItem("laser_pointer", LaserPointerBlock::new,
            softMetal(), true);

    public static final LaserSensorBlock LASER_SENSOR = withItem("laser_sensor", LaserSensorBlock::new,
            wooden().noOcclusion(), false);

    public static final VelocitySensorBlock VELOCITY_SENSOR = withItem("velocity_sensor", VelocitySensorBlock::new,
            wooden().noOcclusion(), true);

    public static final AltitudeSensorBlock ALTITUDE_SENSOR = withItem("altitude_sensor", AltitudeSensorBlock::new,
            wooden().noOcclusion(), true);

    public static final GimbalSensorBlock GIMBAL_SENSOR = withItem("gimbal_sensor", GimbalSensorBlock::new,
            softMetal().noOcclusion().mapColor(MapColor.TERRACOTTA_YELLOW), true);

    public static final NavTableBlock NAVIGATION_TABLE = withItem("navigation_table", NavTableBlock::new,
            wooden().noOcclusion(), true);

    public static final OpticalSensorBlock OPTICAL_SENSOR = withItem("optical_sensor", OpticalSensorBlock::new,
            softMetal().noOcclusion(), false);

    public static final DockingConnectorBlock DOCKING_CONNECTOR = withItem("docking_connector", DockingConnectorBlock::new,
            stone().sound(SoundType.NETHERITE_BLOCK).isRedstoneConductor(SimBlocks::never)
                    .forceSolidOn().noOcclusion().dynamicShape(), true);

    public static final PairedDockingConnectorBlock PAIRED_DOCKING_CONNECTOR = withoutItem("paired_docking_connector", PairedDockingConnectorBlock::new,
            stone().sound(SoundType.NETHERITE_BLOCK).noOcclusion().noLootTable()
                    .pushReaction(PushReaction.IMMOVEABLE).forceSolidOff(), false);

    public static final AnalogTransmissionBlock ANALOG_TRANSMISSION = withItem("analog_transmission", AnalogTransmissionBlock::new,
            stone().noOcclusion().isRedstoneConductor(SimBlocks::never), true);

    public static final SteeringWheelBlock STEERING_WHEEL = withItem("steering_wheel", SteeringWheelBlock::new,
            wooden().mapColor(MapColor.PODZOL), true);

    public static final ThrottleLeverBlock THROTTLE_LEVER = withItem("throttle_lever", ThrottleLeverBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.LEVER), true);

    public static final LinkedTypewriterBlock LINKED_TYPEWRITER = withItem("linked_typewriter", LinkedTypewriterBlock::new,
            softMetal(), true, LinkedTypewriterItem::new);

    public static final DirectionalLinkedReceiverBlock DIRECTIONAL_LINKED_RECEIVER = withItem("directional_linked_receiver", DirectionalLinkedReceiverBlock::new,
            stone().noOcclusion().forceSolidOn(), true);

    public static final ModulatingLinkedReceiverBlock MODULATING_LINKED_RECEIVER = withItem("modulating_linked_receiver", ModulatingLinkedReceiverBlock::new,
            stone().noOcclusion().forceSolidOn(), true);

    public static final RedstoneAccumulatorBlock REDSTONE_ACCUMULATOR = withItem("redstone_accumulator", RedstoneAccumulatorBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.REPEATER), true);

    public static final RedstoneInductorBlock REDSTONE_INDUCTOR = withItem("redstone_inductor", RedstoneInductorBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.REPEATER), true);

    public static final RedstoneMagnetBlock REDSTONE_MAGNET = withItem("redstone_magnet", RedstoneMagnetBlock::new,
            stone().sound(SoundType.NETHERITE_BLOCK).noOcclusion(), true);

    public static final SymmetricSailBlock WHITE_SYMMETRIC_SAIL = withItem("white_symmetric_sail", properties -> SymmetricSailBlock.withCanvas(properties, DyeColor.WHITE),
            wooden().sound(SoundType.SCAFFOLDING).noOcclusion(), false);

    public static final NativeDyedBlocks<SymmetricSailBlock> DYED_SYMMETRIC_SAILS = new NativeDyedBlocks<>(color ->
            color == DyeColor.WHITE ? WHITE_SYMMETRIC_SAIL : withoutItem(color.getSerializedName() + "_symmetric_sail",
                    properties -> SymmetricSailBlock.withCanvas(properties, color), wooden().sound(SoundType.SCAFFOLDING), false));

    public static final NativeDyedBlocks<NameplateBlock> NAMEPLATES = new NativeDyedBlocks<>(color -> {
        final NameplateBlock block = withItem(color.getSerializedName() + "_nameplate",
                properties -> new NameplateBlock(properties, color), wooden().forceSolidOn(), true,
                BlockItem::new, color == DyeColor.WHITE ? Visibility.MAIN : Visibility.SEARCH_ONLY);
        NativeItemCatalog.tooltipKey(block.asItem(), "block.simulated.nameplate");
        return block;
    });

    public static final SpringBlock SPRING = withoutItem("spring", SpringBlock::new,
            softMetal().forceSolidOn(), false);

    private static HandleBlock createHandle(DyeColor color, HandleBlock.Variant variant) {
        final String prefix = variant == HandleBlock.Variant.DYED && color != null ? color.getSerializedName() : variant.getSerializedName();
        final HandleBlock block = withItem(prefix + "_handle", properties -> new HandleBlock(properties, color, variant),
                stone().sound(SoundType.COPPER).noOcclusion().forceSolidOn(), false, BlockItem::new,
                variant == HandleBlock.Variant.IRON ? Visibility.MAIN : Visibility.SEARCH_ONLY);
        NativeItemUseOverrides.addBlock(block);
        NativeItemCatalog.tooltipKey(block.asItem(), "block.simulated.handle");
        return block;
    }

    private static PortableEngineBlock createPortableEngine(DyeColor color) {
        return withItem(color.getSerializedName() + "_portable_engine", properties -> new PortableEngineBlock(properties, color),
                stone().sound(SoundType.NETHERITE_BLOCK).lightLevel(state -> PortableEngineBlock.isLitState(state) ? 6 : 0).noOcclusion(),
                true, BlockItem::new, color == DyeColor.RED ? Visibility.MAIN : Visibility.SEARCH_ONLY);
    }

    private static <B extends Block> B withoutItem(String name, Function<BlockBehaviour.Properties, B> factory,
                                                  BlockBehaviour.Properties properties, boolean cutout) {
        final B block = NativeRegistrations.block(id(name), factory, properties);
        NativeItemCatalog.block(block, cutout);
        return block;
    }

    private static <B extends Block> B withItem(String name, Function<BlockBehaviour.Properties, B> factory,
                                               BlockBehaviour.Properties properties, boolean cutout) {
        return withItem(name, factory, properties, cutout, BlockItem::new, Visibility.MAIN);
    }

    private static <B extends Block> B withItem(String name, Function<BlockBehaviour.Properties, B> factory,
                                               BlockBehaviour.Properties properties, boolean cutout,
                                               BiFunction<Block, Item.Properties, ? extends BlockItem> itemFactory) {
        return withItem(name, factory, properties, cutout, itemFactory, Visibility.MAIN);
    }

    private static <B extends Block> B withItem(String name, Function<BlockBehaviour.Properties, B> factory,
                                               BlockBehaviour.Properties properties, boolean cutout,
                                               BiFunction<Block, Item.Properties, ? extends BlockItem> itemFactory, Visibility visibility) {
        final B block = withoutItem(name, factory, properties, cutout);
        final Item item = NativeRegistrations.item(id(name), itemProperties -> itemFactory.apply(block, itemProperties),
                new Item.Properties().useBlockDescriptionPrefix());
        NativeItemCatalog.add(item, SECTION, visibility);
        return block;
    }

    // Exact SharedProperties sources in upstream Create mc1.21.1: these are vanilla full-copy templates.
    private static BlockBehaviour.Properties wooden() { return BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_SPRUCE_WOOD); }
    private static BlockBehaviour.Properties stone() { return BlockBehaviour.Properties.ofFullCopy(Blocks.ANDESITE); }
    private static BlockBehaviour.Properties softMetal() { return BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_BLOCK); }
    private static BlockBehaviour.Properties netheriteMetal() { return BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK); }
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("simulated", path); }
    private static boolean never(BlockState state, BlockGetter level, BlockPos pos) { return false; }

    public static void register() {
        if (registered) return;
        DisplaySource.BY_BLOCK.register(AUGER_SHAFT, java.util.List.of(SimDisplaySources.AUGER_DISPLAY));
        DisplaySource.BY_BLOCK.register(AUGER_COG, java.util.List.of(SimDisplaySources.AUGER_DISPLAY));
        DisplaySource.BY_BLOCK.register(LASER_SENSOR, java.util.List.of(SimDisplaySources.LASER_SENSOR_DISPLAY));
        DisplaySource.BY_BLOCK.register(VELOCITY_SENSOR, java.util.List.of(SimDisplaySources.VELO_DISPLAY));
        DisplaySource.BY_BLOCK.register(ALTITUDE_SENSOR, java.util.List.of(SimDisplaySources.ALTITUDE_SENSOR_DISPLAY));
        DisplaySource.BY_BLOCK.register(GIMBAL_SENSOR, java.util.List.of(SimDisplaySources.GIMBAL_DISPLAY));
        DisplaySource.BY_BLOCK.register(NAVIGATION_TABLE, java.util.List.of(SimDisplaySources.NAV_TABLE_DISPLAY));
        DisplaySource.BY_BLOCK.register(OPTICAL_SENSOR, java.util.List.of(SimDisplaySources.OPTICAL_SENSOR_DISPLAY));
        DisplaySource.BY_BLOCK.register(DOCKING_CONNECTOR, java.util.List.of(SimDisplaySources.DOCKING_CONNECTOR_DISPLAY));
        DisplaySource.BY_BLOCK.register(LINKED_TYPEWRITER, java.util.List.of(SimDisplaySources.LINKED_TYPEWRITER__DISPLAY));
        MovementBehaviour.REGISTRY.register(ALTITUDE_SENSOR, new AltitudeSensorMovementBehaviour());
        for (PortableEngineBlock block : PORTABLE_ENGINES) {
            BlockStressValues.setGeneratorSpeed(32).accept(block);
            DisplaySource.BY_BLOCK.register(block, java.util.List.of(SimDisplaySources.PORTABLE_ENGINE_DISPLAY));
        }
        BlockStressValues.setGeneratorSpeed(SteeringWheelBlockEntity.RPM).accept(STEERING_WHEEL);
        NativeItemUseOverrides.addBlock(STEERING_WHEEL);
        NativeItemUseOverrides.register();
        for (NameplateBlock block : NAMEPLATES) DisplayTarget.BY_BLOCK.register(block, NameplateBlockTarget.NAMEPLATE);
        RadialWrenchRotation.registerBlacklistedBlock(id("swivel_bearing"));
        RadialWrenchRotation.registerBlacklistedBlock(id("swivel_bearing_link_block"));
        RadialWrenchRotation.registerBlacklistedBlock(id("paired_docking_connector"));
        registered = true;
    }

    private SimBlocks() {}
}
