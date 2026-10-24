package dev.simulated_team.simulated.fabric.service;

import com.simibubi.create.content.contraptions.MountedStorageManager;
import dev.simulated_team.simulated.fabric.storage.FabricInventoryLoaderWrapper;
import dev.simulated_team.simulated.fabric.storage.FabricStorageAdapters;
import dev.simulated_team.simulated.multiloader.energy.SingleBattery;
import dev.simulated_team.simulated.multiloader.inventory.AbstractContainer;
import dev.simulated_team.simulated.multiloader.inventory.InventoryLoaderWrapper;
import dev.simulated_team.simulated.multiloader.tanks.SingleTank;
import dev.simulated_team.simulated.service.SimInventoryService;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;

import java.util.function.BiFunction;
import java.util.function.Consumer;

/** Native sided lookup and mounted-storage integration, with dynamic docking routes. */
public final class FabricSimInventoryService implements SimInventoryService {
    @Override
    public @Nullable InventoryLoaderWrapper getInventory(@Nullable BlockEntity entity, @Nullable Direction side) {
        if (entity == null || entity.isRemoved() || entity.getLevel() == null) return null;
        final var storage = ItemStorage.SIDED.find(entity.getLevel(), entity.getBlockPos(), entity.getBlockState(), entity, side);
        return storage == null ? null : new FabricInventoryLoaderWrapper(storage);
    }

    @Override
    public InventoryLoaderWrapper getWrappedAllItemsFromContraption(MountedStorageManager manager) {
        return new FabricInventoryLoaderWrapper(manager.getAllItems());
    }

    @Override
    public InventoryLoaderWrapper getWrappedMountedItemsFromContraption(MountedStorageManager manager) {
        return new FabricInventoryLoaderWrapper(manager.getMountedItems());
    }

    @Override
    public <T extends BlockEntity> Consumer<BlockEntityType<T>> registerInventory(BiFunction<T, Direction, AbstractContainer> getter) {
        return type -> ItemStorage.SIDED.registerForBlockEntity((entity, side) ->
                entity.isRemoved() || getter.apply(entity, side) == null ? null
                        : FabricStorageAdapters.items(() -> entity.isRemoved() ? null : getter.apply(entity, side)), type);
    }

    @Override
    public <T extends BlockEntity> Consumer<BlockEntityType<T>> registerTank(BiFunction<T, Direction, SingleTank> getter) {
        return type -> FluidStorage.SIDED.registerForBlockEntity((entity, side) ->
                entity.isRemoved() || getter.apply(entity, side) == null ? null
                        : FabricStorageAdapters.fluids(() -> entity.isRemoved() ? null : getter.apply(entity, side), entity::setChanged), type);
    }

    @Override
    public <T extends BlockEntity> Consumer<BlockEntityType<T>> registerBattery(BiFunction<T, Direction, SingleBattery> getter) {
        return type -> EnergyStorage.SIDED.registerForBlockEntity((entity, side) ->
                entity.isRemoved() || getter.apply(entity, side) == null ? null
                        : FabricStorageAdapters.energy(() -> entity.isRemoved() ? null : getter.apply(entity, side), entity::setChanged), type);
    }
}
