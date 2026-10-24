package dev.simulated_team.simulated.fabric.storage;

import dev.simulated_team.simulated.multiloader.inventory.InventoryLoaderWrapper;
import dev.simulated_team.simulated.multiloader.inventory.ItemInfoWrapper;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/** Legacy simulation calls join real native transactions; extraction never mixes data-component variants. */
public final class FabricInventoryLoaderWrapper extends InventoryLoaderWrapper {
    private final Storage<ItemVariant> storage;
    private final Changes changes = new Changes();

    public FabricInventoryLoaderWrapper(Storage<ItemVariant> storage) { this.storage = Objects.requireNonNull(storage); }

    @Override
    public int insertGeneral(ItemInfoWrapper info, int amount, boolean simulate) {
        if (amount <= 0 || info.type() == Items.AIR) return 0;
        final ItemVariant resource = ItemVariant.of(ItemInfoWrapper.generateFromInfo(info));
        try (Transaction transaction = Transaction.openNested(Transaction.getCurrentUnsafe())) {
            int accepted = (int) this.storage.insert(resource, amount, transaction);
            this.finish(transaction, accepted, simulate, false);
            return accepted;
        }
    }

    @Override
    public ItemStack insertSlot(ItemStack stack, int index, boolean simulate) {
        if (stack.isEmpty()) return stack;
        final Storage<ItemVariant> target = this.insertableSlot(index);
        if (target == null) return stack;
        try (Transaction transaction = Transaction.openNested(Transaction.getCurrentUnsafe())) {
            int accepted = (int) target.insert(ItemVariant.of(stack), stack.getCount(), transaction);
            this.finish(transaction, accepted, simulate, false);
            return stack.copyWithCount(stack.getCount() - accepted);
        }
    }

    @Override
    public int extractGeneral(ItemInfoWrapper info, int amount, boolean simulate) {
        if (amount <= 0 || info.type() == Items.AIR) return 0;
        final ItemVariant resource = ItemVariant.of(ItemInfoWrapper.generateFromInfo(info));
        try (Transaction transaction = Transaction.openNested(Transaction.getCurrentUnsafe())) {
            int extracted = (int) this.storage.extract(resource, amount, transaction);
            this.finish(transaction, extracted, simulate, true);
            return extracted;
        }
    }

    @Override
    public ItemStack extractSlot(int index, int amount, boolean simulate) {
        if (amount <= 0) return ItemStack.EMPTY;
        final List<StorageView<ItemVariant>> views = this.views();
        if (index < 0 || index >= views.size()) return ItemStack.EMPTY;
        final StorageView<ItemVariant> view = views.get(index);
        if (view.isResourceBlank()) return ItemStack.EMPTY;
        final ItemVariant resource = view.getResource();
        try (Transaction transaction = Transaction.openNested(Transaction.getCurrentUnsafe())) {
            int extracted = (int) view.extract(resource, amount, transaction);
            this.finish(transaction, extracted, simulate, true);
            return extracted == 0 ? ItemStack.EMPTY : resource.toStack(extracted);
        }
    }

    @Override
    public ItemStack extractAny(int amount, boolean simulate, boolean exact) {
        if (amount <= 0) return ItemStack.EMPTY;
        final var candidates = new LinkedHashSet<ItemVariant>();
        for (var view : this.storage.nonEmptyViews()) if (view.getAmount() > 0) candidates.add(view.getResource());
        for (ItemVariant resource : candidates) {
            try (Transaction transaction = Transaction.openNested(Transaction.getCurrentUnsafe())) {
                final int extracted = (int) this.storage.extract(resource, amount, transaction);
                if (extracted > 0 && (!exact || extracted == amount)) {
                    this.finish(transaction, extracted, simulate, true);
                    return resource.toStack(extracted);
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private void finish(Transaction transaction, int amount, boolean simulate, boolean extraction) {
        if (amount > 0 && !simulate) {
            this.changes.mark(extraction, transaction);
            transaction.commit();
        }
    }

    @Override public int getContainerSize() { return this.storage instanceof SlottedStorage<?> slotted ? slotted.getSlotCount() : this.views().size(); }
    @Override public int getMaxStackSize() {
        var views = this.views(); return views.isEmpty() ? 64 : (int) Math.min(Integer.MAX_VALUE, views.getFirst().getCapacity());
    }
    @Override public boolean isEmpty() {
        for (var view : this.storage.nonEmptyViews()) if (view.getAmount() > 0) return false;
        return true;
    }
    @Override public ItemStack getItem(int index) {
        var views = this.views();
        if (index < 0 || index >= views.size()) return ItemStack.EMPTY;
        var view = views.get(index);
        return view.isResourceBlank() ? ItemStack.EMPTY : view.getResource().toStack((int) Math.min(Integer.MAX_VALUE, view.getAmount()));
    }

    private List<StorageView<ItemVariant>> views() {
        final List<StorageView<ItemVariant>> result = new ArrayList<>();
        this.storage.iterator().forEachRemaining(result::add);
        return result;
    }

    @SuppressWarnings("unchecked")
    private Storage<ItemVariant> insertableSlot(int index) {
        if (index < 0) return null;
        if (this.storage instanceof SlottedStorage<ItemVariant> slotted) return index < slotted.getSlotCount() ? slotted.getSlot(index) : null;
        var views = this.views();
        // Non-slotted aggregate views expose extraction only; do not invent an indexed insertion route.
        return index < views.size() && views.get(index) instanceof Storage<?> target ? (Storage<ItemVariant>) target : null;
    }

    private final class Changes extends SnapshotParticipant<Integer> {
        private int pending;
        private void mark(boolean extraction, TransactionContext transaction) {
            this.updateSnapshots(transaction);
            this.pending |= extraction ? 1 : 2;
        }
        @Override protected Integer createSnapshot() { return this.pending; }
        @Override protected void readSnapshot(Integer previous) { this.pending = previous; }
        @Override protected void onFinalCommit() {
            final int applied = this.pending;
            this.pending = 0;
            if (callback != null) {
                if ((applied & 1) != 0) callback.accept(true);
                if ((applied & 2) != 0) callback.accept(false);
            }
        }
    }
}
