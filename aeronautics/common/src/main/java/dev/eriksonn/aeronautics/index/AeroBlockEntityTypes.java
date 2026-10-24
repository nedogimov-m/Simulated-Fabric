package dev.eriksonn.aeronautics.index;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.simulated_team.simulated.service.SimInventoryService;
import dev.eriksonn.aeronautics.content.blocks.hot_air.hot_air_burner.HotAirBurnerBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.hot_air.steam_vent.SteamVentBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.gyroscopic_propeller_bearing.GyroscopicPropellerBearingBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing.PropellerBearingBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.andesite.AndesitePropellerBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.smart_propeller.SmartPropellerBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.wooden.WoodenPropellerBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.mounted_potato_cannon.MountedPotatoCannonBlockEntity;
import dev.simulated_team.simulated.fabric.registry.NativeBlockEntities;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Actual common factories. Renderer and former instancing bindings belong to the native client helper. */
public final class AeroBlockEntityTypes {
    private static boolean registered;
    public static final BlockEntityType<SteamVentBlockEntity> STEAM_VENT = NativeBlockEntities.register(
            id("steam_vent"), SteamVentBlockEntity::new, AeroBlocks.STEAM_VENT);
    public static final BlockEntityType<HotAirBurnerBlockEntity> HOT_AIR_BURNER = NativeBlockEntities.register(
            id("adjustable_burner"), HotAirBurnerBlockEntity::new, AeroBlocks.HOT_AIR_BURNER);
    public static final BlockEntityType<PropellerBearingBlockEntity> PROPELLER_BEARING = NativeBlockEntities.register(
            id("propeller_bearing"), PropellerBearingBlockEntity::new, AeroBlocks.PROPELLER_BEARING);
    public static final BlockEntityType<GyroscopicPropellerBearingBlockEntity> GYROSCOPIC_PROPELLER_BEARING = NativeBlockEntities.register(
            id("gyroscopic_propeller_bearing"), GyroscopicPropellerBearingBlockEntity::new, AeroBlocks.GYROSCOPIC_PROPELLER_BEARING);
    public static final BlockEntityType<KineticBlockEntity> ENVELOPE_ENCASED_SHAFT = NativeBlockEntities.register(
            id("envelope_encased_shaft"), KineticBlockEntity::new, AeroBlocks.ENVELOPE_ENCASED_SHAFTS.toArray());
    public static final BlockEntityType<AndesitePropellerBlockEntity> ANDESITE_PROPELLER = NativeBlockEntities.register(
            id("andesite_propeller"), AndesitePropellerBlockEntity::new, AeroBlocks.ANDESITE_PROPELLER);
    public static final BlockEntityType<WoodenPropellerBlockEntity> WOODEN_PROPELLER = NativeBlockEntities.register(
            id("wooden_propeller"), WoodenPropellerBlockEntity::new, AeroBlocks.WOODEN_PROPELLER);
    public static final BlockEntityType<SmartPropellerBlockEntity> SMART_PROPELLER = NativeBlockEntities.register(
            id("smart_propeller"), SmartPropellerBlockEntity::new, AeroBlocks.SMART_PROPELLER);
    public static final BlockEntityType<MountedPotatoCannonBlockEntity> MOUNTED_POTATO_CANNON = NativeBlockEntities.register(
            id("mounted_potato_cannon"), MountedPotatoCannonBlockEntity::new, AeroBlocks.MOUNTED_POTATO_CANNON);

    public static void init() {
        if (registered) return;
        SimInventoryService.INSTANCE.<MountedPotatoCannonBlockEntity>registerInventory((be, side) -> be.getInventory())
                .accept(MOUNTED_POTATO_CANNON);
        registered = true;
    }
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("aeronautics", path); }
    private AeroBlockEntityTypes() {}
}
