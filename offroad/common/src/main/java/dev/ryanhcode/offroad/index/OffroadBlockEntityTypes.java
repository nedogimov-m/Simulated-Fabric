package dev.ryanhcode.offroad.index;

import dev.ryanhcode.offroad.content.blocks.borehead_bearing.BoreheadBearingBlockEntity;
import dev.ryanhcode.offroad.content.blocks.rock_cutting_wheel.RockCuttingWheelBlockEntity;
import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountBlockEntity;
import dev.simulated_team.simulated.fabric.registry.NativeBlockEntities;
import dev.simulated_team.simulated.service.SimInventoryService;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Common factories and actual inventory providers; renderer registrations live in the client helper. */
public final class OffroadBlockEntityTypes {
    private static boolean registered;
    public static final BlockEntityType<WheelMountBlockEntity> WHEEL_MOUNT = NativeBlockEntities.register(
            id("wheel_mount"), WheelMountBlockEntity::new, OffroadBlocks.WHEEL_MOUNT);
    public static final BlockEntityType<BoreheadBearingBlockEntity> BOREHEAD_BEARING = NativeBlockEntities.register(
            id("borehead_bearing"), BoreheadBearingBlockEntity::new, OffroadBlocks.BOREHEAD_BEARING_BLOCK);
    public static final BlockEntityType<RockCuttingWheelBlockEntity> ROCKCUTTING_WHEEL_BLOCK_ENTITY = NativeBlockEntities.register(
            id("rockcutting_wheel"), RockCuttingWheelBlockEntity::new, OffroadBlocks.ROCK_CUTTER_BLOCK);

    public static void init() {
        if (registered) return;
        SimInventoryService.INSTANCE.<WheelMountBlockEntity>registerInventory((be, side) -> be.getInventory())
                .accept(WHEEL_MOUNT);
        SimInventoryService.INSTANCE.<BoreheadBearingBlockEntity>registerInventory((be, side) -> be.getContraptionWrappedInventory())
                .accept(BOREHEAD_BEARING);
        registered = true;
    }

    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("offroad", path); }
    private OffroadBlockEntityTypes() {}
}
