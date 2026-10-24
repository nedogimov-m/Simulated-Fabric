package dev.ryanhcode.offroad.index;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.actors.roller.RollerBlockItem;
import dev.ryanhcode.offroad.content.blocks.borehead_bearing.BoreheadBearingBlock;
import dev.ryanhcode.offroad.content.blocks.rock_cutting_wheel.RockCuttingWheelActor;
import dev.ryanhcode.offroad.content.blocks.rock_cutting_wheel.RockCuttingWheelBlock;
import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountBlock;
import dev.ryanhcode.offroad.content.components.TireLike;
import dev.simulated_team.simulated.fabric.registry.NativeItemCatalog;
import dev.simulated_team.simulated.fabric.registry.NativeRegistrations;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.function.BiFunction;
import java.util.function.Function;

/** Native registrations; the original generated models, loot, tags and recipes remain authoritative. */
public final class OffroadBlocks {
    private static final Identifier SECTION = id("offroad");
    private static boolean registered;

    public static final BoreheadBearingBlock BOREHEAD_BEARING_BLOCK = withItem("borehead_bearing",
            BoreheadBearingBlock::new, softMetal().isRedstoneConductor((state, level, pos) -> false),
            BlockItem::new, new Item.Properties());
    public static final RockCuttingWheelBlock ROCK_CUTTER_BLOCK = withItem("rockcutting_wheel",
            RockCuttingWheelBlock::new, softMetal(), BlockItem::new,
            new Item.Properties().component(OffroadDataComponents.TIRE, TireLike.ROCKCUTTING_WHEEL));
    public static final WheelMountBlock WHEEL_MOUNT = withItem("wheel_mount", WheelMountBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.ANDESITE).mapColor(MapColor.COLOR_GRAY)
                    .noOcclusion().isRedstoneConductor((state, level, pos) -> false),
            RollerBlockItem::new, new Item.Properties());

    private static BlockBehaviour.Properties softMetal() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_BLOCK).mapColor(MapColor.COLOR_GRAY)
                .sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops().noOcclusion();
    }

    private static <B extends Block> B withItem(String name, Function<BlockBehaviour.Properties, B> factory,
                                                BlockBehaviour.Properties properties,
                                                BiFunction<Block, Item.Properties, ? extends BlockItem> itemFactory,
                                                Item.Properties itemProperties) {
        final B block = NativeRegistrations.block(id(name), factory, properties);
        final BlockItem item = NativeRegistrations.item(id(name), p -> itemFactory.apply(block, p),
                itemProperties.useBlockDescriptionPrefix());
        NativeItemCatalog.block(block, true);
        NativeItemCatalog.add(item, SECTION, NativeItemCatalog.Visibility.MAIN);
        return block;
    }

    public static void init() {
        if (registered) return;
        MovementBehaviour.REGISTRY.register(ROCK_CUTTER_BLOCK, new RockCuttingWheelActor());
        registered = true;
    }

    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("offroad", path); }
    private OffroadBlocks() {}
}
