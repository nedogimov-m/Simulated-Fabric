package dev.ryanhcode.offroad.fabric.storage;

import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.content.contraptions.MountedStorageManager;
import dev.ryanhcode.offroad.content.blocks.borehead_bearing.BoreheadAttachedStorage;
import dev.ryanhcode.offroad.content.blocks.borehead_bearing.BoreheadBearingBlockEntity;
import dev.ryanhcode.offroad.service.OffroadMountedStorageService;
import java.lang.ref.WeakReference;

public final class FabricOffroadMountedStorageService implements OffroadMountedStorageService {
    @Override @SuppressWarnings("unchecked")
    public <T extends MountedStorageManager & BoreheadAttachedStorage> T getSidedBoreheadContraptionMountedStorage() { return (T)new Manager(); }
    public static final class Manager extends MountedStorageManager implements BoreheadAttachedStorage {
        private WeakReference<BoreheadBearingBlockEntity> attached=new WeakReference<>(null);
        private final BoreheadStoragePolicy policy=new BoreheadStoragePolicy(()->this.attached.get()!=null,this::invokeUnstall);
        @Override public void initialize() {
            super.initialize();
            for(var storage:this.getAllItemStorages().values()) {
                ((PolicyAwareMountedStorage)storage).offroad$bindPolicy(this.policy);
                final long[] previous=amounts(storage);
                storage.bindExchangeObserver(transaction->transaction.addOuterCloseCallback(result->{
                    if(result.wasCommitted())System.arraycopy(amounts(storage),0,previous,0,previous.length);
                }),()->{
                    final long[] current=amounts(storage);boolean freed=false;
                    for(int slot=0;slot<current.length;slot++){freed|=current[slot]<previous[slot];previous[slot]=current[slot];}
                    if(freed)invokeUnstall();
                });
            }
        }
        private static long[] amounts(MountedItemStorage storage) {
            var values=new long[storage.getSlotCount()];for(int slot=0;slot<values.length;slot++)values[slot]=storage.getSlot(slot).getAmount();return values;
        }
        @Override public void attachBlockEntity(BoreheadBearingBlockEntity bearing) { this.attached=new WeakReference<>(bearing); }
        @Override public void setInsertAllowed(boolean allowed) { this.policy.insertionAllowed(allowed); }
        @Override public void invokeUnstall() { var bearing=this.attached.get();if(bearing!=null)bearing.startUnstalling(); }
    }
}
