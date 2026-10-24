package dev.eriksonn.aeronautics.index;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.decoration.encasing.EncasingRegistry;
import dev.eriksonn.aeronautics.content.blocks.hot_air.envelope.EnvelopeBlock;
import dev.eriksonn.aeronautics.content.blocks.hot_air.envelope.EnvelopeEncasedShaftBlock;
import dev.eriksonn.aeronautics.content.blocks.hot_air.hot_air_burner.HotAirBurnerBlock;
import dev.eriksonn.aeronautics.content.blocks.hot_air.steam_vent.SteamVentBlock;
import dev.eriksonn.aeronautics.content.blocks.mounted_potato_cannon.MountedPotatoCannonBlock;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.gyroscopic_propeller_bearing.GyroscopicPropellerBearingBlock;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing.PropellerBearingBlock;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.andesite.AndesitePropellerBlock;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.smart_propeller.SmartPropellerBlock;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.wooden.WoodenPropellerBlock;
import dev.eriksonn.aeronautics.content.components.Levitating;
import dev.simulated_team.simulated.fabric.registry.NativeDyedBlocks;
import dev.simulated_team.simulated.fabric.registry.NativeItemCatalog;
import dev.simulated_team.simulated.fabric.registry.NativeItemCatalog.Visibility;
import dev.simulated_team.simulated.fabric.registry.NativeRegistrations;
import dev.simulated_team.simulated.index.sounds.SimLazySoundType;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;

/** Native runtime blocks. Checked-in generated recipes, blockstates, models, loot and tags are retained. */
public final class AeroBlocks {
    private static final Identifier SECTION = id("aeronautics");
    private static boolean registered;

    public static final EnvelopeBlock WHITE_ENVELOPE_BLOCK = envelope(DyeColor.WHITE);
    public static final NativeDyedBlocks<EnvelopeBlock> DYED_ENVELOPE_BLOCKS = new NativeDyedBlocks<>(color ->
            color == DyeColor.WHITE ? WHITE_ENVELOPE_BLOCK : envelope(color));
    public static final NativeDyedBlocks<EnvelopeEncasedShaftBlock> ENVELOPE_ENCASED_SHAFTS = new NativeDyedBlocks<>(color ->
            withItem(color.getSerializedName() + "_envelope_encased_shaft",
                    properties -> EnvelopeEncasedShaftBlock.withCanvas(properties, color),
                    wooden().sound(envelopeSound()).noOcclusion().mapColor(color),
                    new Item.Properties(), Visibility.INVISIBLE));

    public static final HotAirBurnerBlock HOT_AIR_BURNER = withItem("adjustable_burner", HotAirBurnerBlock::new,
            stone().sound(SoundType.NETHERITE_BLOCK).noOcclusion().lightLevel(HotAirBurnerBlock::getLightPower));
    public static final SteamVentBlock STEAM_VENT = withItem("steam_vent", SteamVentBlock::new,
            stone().sound(SoundType.NETHERITE_BLOCK).noOcclusion());
    public static final PropellerBearingBlock PROPELLER_BEARING = withItem("propeller_bearing", PropellerBearingBlock::new,
            stone().sound(SoundType.COPPER).noOcclusion());
    public static final GyroscopicPropellerBearingBlock GYROSCOPIC_PROPELLER_BEARING = withItem("gyroscopic_propeller_bearing",
            GyroscopicPropellerBearingBlock::new, stone().sound(SoundType.COPPER).noOcclusion());
    public static final SmartPropellerBlock SMART_PROPELLER = withItem("smart_propeller", SmartPropellerBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_BLOCK));
    public static final AndesitePropellerBlock ANDESITE_PROPELLER = withItem("andesite_propeller", AndesitePropellerBlock::new,
            wooden().sound(SoundType.WOOD));
    public static final WoodenPropellerBlock WOODEN_PROPELLER = withItem("wooden_propeller", WoodenPropellerBlock::new,
            wooden().sound(SoundType.WOOD));
    public static final MountedPotatoCannonBlock MOUNTED_POTATO_CANNON = withItem("mounted_potato_cannon", MountedPotatoCannonBlock::new,
            stone().noOcclusion());
    public static final Block LEVITITE = withItem("levitite", Block::new, levitite(),
            new Item.Properties().component(AeroDataComponents.LEVITATING, Levitating.LEVITITE), Visibility.MAIN);
    public static final Block PEARLESCENT_LEVITITE = withItem("pearlescent_levitite", Block::new, levitite(),
            new Item.Properties().component(AeroDataComponents.LEVITATING, Levitating.PEARLESCENT_LEVITITE), Visibility.MAIN);

    private static EnvelopeBlock envelope(DyeColor color) {
        return withItem(color.getSerializedName() + "_envelope", properties -> new EnvelopeBlock(properties, color),
                wooden().isValidSpawn((state, level, pos, entity) -> false).sound(envelopeSound()).mapColor(color),
                new Item.Properties(), color == DyeColor.WHITE ? Visibility.MAIN : Visibility.SEARCH_ONLY);
    }
    private static SoundType envelopeSound() {
        return new SimLazySoundType(1, 1, AeroSoundEvents.ENVELOPE_BREAK::event, () -> SoundEvents.WOOL_STEP,
                AeroSoundEvents.ENVELOPE_PLACE::event, AeroSoundEvents.ENVELOPE_HIT::event, () -> SoundEvents.WOOL_FALL);
    }
    private static BlockBehaviour.Properties levitite() {
        return BlockBehaviour.Properties.of().lightLevel(state -> 10).noLootTable().strength(7, 20)
                .sound(new SimLazySoundType(1, 1, AeroSoundEvents.LEVITITE_BREAK::event, () -> SoundEvents.AMETHYST_BLOCK_STEP,
                        AeroSoundEvents.LEVITITE_PLACE::event, () -> SoundEvents.AMETHYST_BLOCK_HIT, () -> SoundEvents.AMETHYST_BLOCK_FALL));
    }
    private static BlockBehaviour.Properties wooden() { return BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_SPRUCE_WOOD); }
    private static BlockBehaviour.Properties stone() { return BlockBehaviour.Properties.ofFullCopy(Blocks.ANDESITE); }
    private static <B extends Block> B withItem(String name, Function<BlockBehaviour.Properties, B> factory,
                                                BlockBehaviour.Properties properties) {
        return withItem(name, factory, properties, new Item.Properties(), Visibility.MAIN);
    }
    private static <B extends Block> B withItem(String name, Function<BlockBehaviour.Properties, B> factory,
                                                BlockBehaviour.Properties properties, Item.Properties itemProperties,
                                                Visibility visibility) {
        final B block = NativeRegistrations.block(id(name), factory, properties);
        final BlockItem item = NativeRegistrations.item(id(name), p -> new BlockItem(block, p),
                itemProperties.useBlockDescriptionPrefix());
        NativeItemCatalog.block(block, false);
        NativeItemCatalog.add(item, SECTION, visibility);
        return block;
    }

    public static void init() {
        if (registered) return;
        for (EnvelopeBlock block : DYED_ENVELOPE_BLOCKS) FlammableBlockRegistry.getDefaultInstance().add(block, 30, 60);
        for (EnvelopeEncasedShaftBlock block : ENVELOPE_ENCASED_SHAFTS) EncasingRegistry.addVariant(AllBlocks.SHAFT, block);
        DisplaySource.BY_BLOCK.register(HOT_AIR_BURNER, java.util.List.of(AeroDisplaySources.GAS_DISPLAY));
        DisplaySource.BY_BLOCK.register(STEAM_VENT, java.util.List.of(AeroDisplaySources.GAS_DISPLAY));
        registered = true;
    }

    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("aeronautics", path); }
    private AeroBlocks() {}
}
