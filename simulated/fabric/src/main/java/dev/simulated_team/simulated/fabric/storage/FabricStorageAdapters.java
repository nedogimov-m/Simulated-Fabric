package dev.simulated_team.simulated.fabric.storage;

import dev.simulated_team.simulated.multiloader.energy.SingleBattery;
import dev.simulated_team.simulated.multiloader.inventory.AbstractContainer;
import dev.simulated_team.simulated.multiloader.inventory.ContainerSlot;
import dev.simulated_team.simulated.multiloader.tanks.CFluidType;
import dev.simulated_team.simulated.multiloader.tanks.SingleTank;
import dev.simulated_team.simulated.service.SimInventoryService.RoutedBattery;
import dev.simulated_team.simulated.service.SimInventoryService.RoutedTank;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import team.reborn.energy.api.EnergyStorage;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/** Dynamic API facades resolve current docking routes; physical adapters own the transaction snapshots. */
public final class FabricStorageAdapters {
    private static final Map<AbstractContainer, WeakReference<FabricContainerStorage>> ITEMS = new WeakHashMap<>();
    private static final Map<SingleTank, WeakReference<FabricSingleTankStorage>> TANKS = new WeakHashMap<>();
    private static final Map<SingleTank, WeakReference<FabricSingleTankStorage>> PHYSICAL_TANKS = new WeakHashMap<>();
    private static final Map<SingleBattery, WeakReference<FabricBatteryStorage>> BATTERIES = new WeakHashMap<>();
    private static final Map<SingleBattery, WeakReference<FabricBatteryStorage>> PHYSICAL_BATTERIES = new WeakHashMap<>();

    private static synchronized FabricContainerStorage items(AbstractContainer container) {
        var reference = ITEMS.get(container);
        var storage = reference == null ? null : reference.get();
        if (storage == null) { storage = new FabricContainerStorage(container); ITEMS.put(container, new WeakReference<>(storage)); }
        return storage;
    }

    private static synchronized FabricSingleTankStorage tank(SingleTank tank, boolean physical, Runnable ownerChanged) {
        var cache = physical ? PHYSICAL_TANKS : TANKS;
        var reference = cache.get(tank);
        var storage = reference == null ? null : reference.get();
        if (storage == null) {
            final Runnable changed = tank instanceof RoutedTank routed ? routed::onTankCommit : ownerChanged;
            storage = physical ? FabricSingleTankStorage.forPhysicalTank(tank, changed)
                    : FabricSingleTankStorage.withCommitCallback(tank, changed);
            cache.put(tank, new WeakReference<>(storage));
        }
        return storage;
    }

    private static synchronized FabricBatteryStorage battery(SingleBattery battery, boolean physical, Runnable ownerChanged) {
        var cache = physical ? PHYSICAL_BATTERIES : BATTERIES;
        var reference = cache.get(battery);
        var storage = reference == null ? null : reference.get();
        if (storage == null) {
            final Runnable changed = battery instanceof RoutedBattery routed ? routed::onBatteryCommit : ownerChanged;
            storage = new FabricBatteryStorage(battery, changed, physical);
            cache.put(battery, new WeakReference<>(storage));
        }
        return storage;
    }

    public static SlottedStorage<ItemVariant> items(Supplier<AbstractContainer> getter) { return new DynamicItems(getter); }
    public static Storage<FluidVariant> fluids(Supplier<SingleTank> getter, Runnable changed) { return new DynamicFluids(getter, changed); }
    public static EnergyStorage energy(Supplier<SingleBattery> getter, Runnable changed) { return new DynamicEnergy(getter, changed); }

    private static final class DynamicItems implements SlottedStorage<ItemVariant> {
        private final Supplier<AbstractContainer> getter;
        private List<ContainerSlot> view;
        private FabricContainerStorage current;
        private DynamicItems(Supplier<AbstractContainer> getter) { this.getter = getter; }
        private FabricContainerStorage resolve() {
            final AbstractContainer inventory = this.getter.get();
            if (inventory == null) { this.view = null; this.current = null; return null; }
            final List<ContainerSlot> view = inventory.getInventoryAsList();
            // Docking returns a new facade each time, with the same ordered physical slots until its peer changes.
            if (this.current == null || view.isEmpty() || !view.equals(this.view)) {
                this.current = FabricStorageAdapters.items(inventory);
                this.view = List.copyOf(view);
            }
            return this.current;
        }
        @Override public boolean supportsInsertion() { return this.resolve() != null; }
        @Override public boolean supportsExtraction() { return this.resolve() != null; }
        @Override public long insert(ItemVariant resource, long amount, TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, amount);
            var storage = this.resolve(); return storage == null ? 0 : storage.insert(resource, amount, transaction);
        }
        @Override public long extract(ItemVariant resource, long amount, TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, amount);
            var storage = this.resolve(); return storage == null ? 0 : storage.extract(resource, amount, transaction);
        }
        @Override public int getSlotCount() { var storage = this.resolve(); return storage == null ? 0 : storage.getSlotCount(); }
        @Override public SingleSlotStorage<ItemVariant> getSlot(int index) {
            if (index < 0 || index >= this.getSlotCount()) throw new IndexOutOfBoundsException(index);
            return new DynamicSlot(index);
        }
        @Override public Iterator<StorageView<ItemVariant>> iterator() {
            List<StorageView<ItemVariant>> views = new ArrayList<>();
            for (int index = 0, count = this.getSlotCount(); index < count; index++) views.add(new DynamicSlot(index));
            return views.iterator();
        }
        private final class DynamicSlot implements SingleSlotStorage<ItemVariant> {
            private final int index;
            private DynamicSlot(int index) { this.index = index; }
            private SingleSlotStorage<ItemVariant> resolveSlot() {
                final var storage = resolve();
                return storage == null || this.index >= storage.getSlotCount() ? null : storage.getSlot(this.index);
            }
            @Override public long insert(ItemVariant resource, long amount, TransactionContext transaction) {
                StoragePreconditions.notBlankNotNegative(resource, amount);
                var slot = this.resolveSlot(); return slot == null ? 0 : slot.insert(resource, amount, transaction);
            }
            @Override public long extract(ItemVariant resource, long amount, TransactionContext transaction) {
                StoragePreconditions.notBlankNotNegative(resource, amount);
                var slot = this.resolveSlot(); return slot == null ? 0 : slot.extract(resource, amount, transaction);
            }
            @Override public boolean isResourceBlank() { var slot = this.resolveSlot(); return slot == null || slot.isResourceBlank(); }
            @Override public ItemVariant getResource() { var slot = this.resolveSlot(); return slot == null ? ItemVariant.blank() : slot.getResource(); }
            @Override public long getAmount() { var slot = this.resolveSlot(); return slot == null ? 0 : slot.getAmount(); }
            @Override public long getCapacity() { var slot = this.resolveSlot(); return slot == null ? 0 : slot.getCapacity(); }
        }
    }

    private static final class DynamicFluids implements Storage<FluidVariant> {
        private final Supplier<SingleTank> getter;
        private final Runnable changed;
        private DynamicFluids(Supplier<SingleTank> getter, Runnable changed) { this.getter = getter; this.changed = changed; }
        @Override public boolean supportsInsertion() {
            var source = this.getter.get(); return source != null && (!(source instanceof RoutedTank routed) || routed.insertionTank() != null);
        }
        @Override public boolean supportsExtraction() { return this.getter.get() != null; }
        @Override public long insert(FluidVariant resource, long amount, TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, amount);
            var source = this.getter.get();
            if (source == null) return 0;
            var target = source instanceof RoutedTank routed ? routed.insertionTank() : source;
            return target == null ? 0 : tank(target, source instanceof RoutedTank || target instanceof RoutedTank, this.changed).insert(resource, amount, transaction);
        }
        @Override public long extract(FluidVariant resource, long amount, TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, amount);
            var source = this.getter.get();
            return source == null ? 0 : tank(source, source instanceof RoutedTank, this.changed).extract(resource, amount, transaction);
        }
        @Override public Iterator<StorageView<FluidVariant>> iterator() {
            return List.<StorageView<FluidVariant>>of(new StorageView<>() {
                @Override public long extract(FluidVariant resource, long amount, TransactionContext transaction) { return DynamicFluids.this.extract(resource, amount, transaction); }
                @Override public boolean isResourceBlank() { var tank = getter.get(); return tank == null || tank.type.isBlank(); }
                @Override public FluidVariant getResource() { var tank = getter.get(); var type = tank == null ? CFluidType.BLANK : tank.type; return FluidVariant.of(type.fluid(), type.data()); }
                @Override public long getAmount() { var tank = getter.get(); return tank == null ? 0 : tank.amount; }
                @Override public long getCapacity() { var tank = getter.get(); return tank == null ? 0 : tank.capacity; }
            }).iterator();
        }
    }

    private static final class DynamicEnergy implements EnergyStorage {
        private final Supplier<SingleBattery> getter;
        private final Runnable changed;
        private DynamicEnergy(Supplier<SingleBattery> getter, Runnable changed) { this.getter = getter; this.changed = changed; }
        @Override public boolean supportsInsertion() {
            var source = this.getter.get(); return source != null && source.canReceive()
                    && (!(source instanceof RoutedBattery routed) || routed.insertionBattery() != null);
        }
        @Override public boolean supportsExtraction() { var source = this.getter.get(); return source != null && source.canExtract(); }
        @Override public long insert(long amount, TransactionContext transaction) {
            StoragePreconditions.notNegative(amount);
            var source = this.getter.get();
            if (source == null || !source.canReceive()) return 0;
            var target = source instanceof RoutedBattery routed ? routed.insertionBattery() : source;
            return target == null ? 0 : battery(target, source instanceof RoutedBattery || target instanceof RoutedBattery, this.changed).insert(amount, transaction);
        }
        @Override public long extract(long amount, TransactionContext transaction) {
            StoragePreconditions.notNegative(amount);
            var source = this.getter.get();
            return source == null ? 0 : battery(source, source instanceof RoutedBattery, this.changed).extract(amount, transaction);
        }
        @Override public long getAmount() { var source = this.getter.get(); return source == null ? 0 : source.getEnergy(); }
        @Override public long getCapacity() { var source = this.getter.get(); return source == null ? 0 : source.maxEnergy; }
    }

    private FabricStorageAdapters() {}
}
