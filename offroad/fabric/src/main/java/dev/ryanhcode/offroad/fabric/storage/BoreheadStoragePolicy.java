package dev.ryanhcode.offroad.fabric.storage;

import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import java.util.function.BooleanSupplier;

/** Policy on genuine native transfer handles; successful extraction wakes the controller only at final commit. */
public final class BoreheadStoragePolicy {
    private final BooleanSupplier attached;
    private final Runnable unstall;
    private boolean insertionAllowed;
    private boolean pendingWakeup;
    private final SnapshotParticipant<Boolean> wakeup=new SnapshotParticipant<>() {
        @Override protected Boolean createSnapshot() { return pendingWakeup; }
        @Override protected void readSnapshot(Boolean value) { pendingWakeup=value; }
        @Override protected void onFinalCommit() { if(pendingWakeup){pendingWakeup=false;unstall.run();} }
    };
    public BoreheadStoragePolicy(BooleanSupplier attached,Runnable unstall) { this.attached=attached;this.unstall=unstall; }
    public boolean permitsInsertion() { return this.insertionAllowed; }
    public boolean permitsExtraction() { return this.attached.getAsBoolean(); }
    public void insertionAllowed(boolean value) { this.insertionAllowed=value; }
    public void extracted(long amount,TransactionContext transaction) {
        if(amount>0) { this.wakeup.updateSnapshots(transaction);this.pendingWakeup=true; }
    }
    public <T extends TransferVariant<?>> StorageView<T> view(StorageView<T> original) { return new View<>(original); }
    public <T extends TransferVariant<?>> SingleSlotStorage<T> slot(SingleSlotStorage<T> original) { return new Slot<>(original); }
    private class View<T extends TransferVariant<?>> implements StorageView<T> {
        protected final StorageView<T> original;
        private View(StorageView<T> original) { this.original=original; }
        @Override public boolean isResourceBlank() { return this.original.isResourceBlank(); }
        @Override public T getResource() { return this.original.getResource(); }
        @Override public long getAmount() { return this.original.getAmount(); }
        @Override public long getCapacity() { return this.original.getCapacity(); }
        @Override public StorageView<T> getUnderlyingView() { return this.original.getUnderlyingView(); }
        @Override public long extract(T resource,long amount,TransactionContext transaction) {
            if(!permitsExtraction())return 0;
            long moved=this.original.extract(resource,amount,transaction);extracted(moved,transaction);return moved;
        }
    }
    private final class Slot<T extends TransferVariant<?>> extends View<T> implements SingleSlotStorage<T> {
        private final SingleSlotStorage<T> slot;
        private Slot(SingleSlotStorage<T> original) { super(original);this.slot=original; }
        @Override public long insert(T resource,long amount,TransactionContext transaction) { return permitsInsertion()?this.slot.insert(resource,amount,transaction):0; }
        // Native guardSlot already routes extraction through the policy-wrapped guard view.
        @Override public long extract(T resource,long amount,TransactionContext transaction) { return permitsExtraction()?this.slot.extract(resource,amount,transaction):0; }
        @Override public boolean supportsInsertion() { return permitsInsertion()&&this.slot.supportsInsertion(); }
        @Override public boolean supportsExtraction() { return permitsExtraction()&&this.slot.supportsExtraction(); }
        @Override public long getVersion() { return this.slot.getVersion(); }
    }
}
