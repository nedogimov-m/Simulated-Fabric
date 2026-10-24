package dev.simulated_team.simulated.fabric.storage;

import dev.simulated_team.simulated.multiloader.inventory.AbstractContainer;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/** A native Fabric transaction view over a Simulated inventory, including nested rollback. */
public final class FabricContainerStorage extends SnapshotParticipant<List<ItemStack>> implements SlottedStorage<ItemVariant> {
    private final AbstractContainer container;
    private final List<SingleSlotStorage<ItemVariant>> slots;

    public FabricContainerStorage(AbstractContainer container) {
        this.container = Objects.requireNonNull(container);
        final List<SingleSlotStorage<ItemVariant>> slots = new ArrayList<>();
        for (int index = 0; index < container.getContainerSize(); index++) {
            slots.add(new Slot(index));
        }
        this.slots = List.copyOf(slots);
    }

    @Override
    public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);
        long moved = 0;
        for (final var slot : this.slots) {
            if (moved == maxAmount) break;
            moved += slot.insert(resource, maxAmount - moved, transaction);
        }
        return moved;
    }

    @Override
    public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);
        long moved = 0;
        for (final var slot : this.slots) {
            if (moved == maxAmount) break;
            moved += slot.extract(resource, maxAmount - moved, transaction);
        }
        return moved;
    }

    @Override
    public int getSlotCount() { return this.slots.size(); }

    @Override
    public SingleSlotStorage<ItemVariant> getSlot(int index) { return this.slots.get(index); }

    @Override
    public Iterator<StorageView<ItemVariant>> iterator() {
        return this.slots.stream().map(slot -> (StorageView<ItemVariant>) slot).iterator();
    }

    @Override
    protected List<ItemStack> createSnapshot() {
        return this.slots.stream().map(slot -> this.container.getItem(((Slot) slot).index).copy()).toList();
    }

    @Override
    protected void readSnapshot(List<ItemStack> snapshot) {
        try (var ignored = AbstractContainer.suppressChangeNotifications()) {
            for (int index = 0; index < snapshot.size(); index++) {
                this.container.setItem(index, snapshot.get(index).copy());
            }
        }
    }

    @Override
    protected void onFinalCommit() {
        this.container.setChanged();
    }

    private final class Slot implements SingleSlotStorage<ItemVariant> {
        private final int index;
        private Slot(int index) { this.index = index; }

        @Override
        public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);
            final ItemStack requested = resource.toStack((int) Math.min(maxAmount, Integer.MAX_VALUE));
            final int accepted = requested.getCount() - container.insertSlot(requested, this.index, true).getCount();
            if (accepted > 0) {
                updateSnapshots(transaction);
                try (var ignored = AbstractContainer.suppressChangeNotifications()) {
                    container.insertSlot(resource.toStack(accepted), this.index, false);
                }
            }
            return accepted;
        }

        @Override
        public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);
            if (!resource.matches(container.getItem(this.index))) return 0;
            final ItemStack extracted = container.extractSlot(this.index, (int) Math.min(maxAmount, Integer.MAX_VALUE), true);
            if (!extracted.isEmpty()) {
                updateSnapshots(transaction);
                try (var ignored = AbstractContainer.suppressChangeNotifications()) {
                    container.extractSlot(this.index, extracted.getCount(), false);
                }
            }
            return extracted.getCount();
        }

        @Override
        public boolean isResourceBlank() { return container.getItem(this.index).isEmpty(); }

        @Override
        public ItemVariant getResource() { return ItemVariant.of(container.getItem(this.index)); }

        @Override
        public long getAmount() { return container.getItem(this.index).getCount(); }

        @Override
        public long getCapacity() {
            final ItemStack stack = container.getItem(this.index);
            return stack.isEmpty() ? container.getMaxStackSize() : container.getMaxStackSize(stack);
        }
    }
}
