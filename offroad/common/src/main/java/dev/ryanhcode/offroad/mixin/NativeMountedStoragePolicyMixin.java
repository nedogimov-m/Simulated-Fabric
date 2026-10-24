package dev.ryanhcode.offroad.mixin;

import com.simibubi.create.impl.contraption.storage.MountedStorageAccess;
import dev.ryanhcode.offroad.fabric.storage.BoreheadStoragePolicy;
import dev.ryanhcode.offroad.fabric.storage.PolicyAwareMountedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=MountedStorageAccess.class,remap=false)
public abstract class NativeMountedStoragePolicyMixin<T extends TransferVariant<?>> implements PolicyAwareMountedStorage {
    @Unique private BoreheadStoragePolicy offroad$policy;
    @Override public void offroad$bindPolicy(BoreheadStoragePolicy policy) { this.offroad$policy=policy; }
    @Inject(method="insert",at=@At("HEAD"),cancellable=true,require=1)
    private void offroad$insert(T resource,long amount,TransactionContext transaction,CallbackInfoReturnable<Long> result) {
        if(this.offroad$policy!=null&&!this.offroad$policy.permitsInsertion())result.setReturnValue(0L);
    }
    @Inject(method="extract",at=@At("HEAD"),cancellable=true,require=1)
    private void offroad$extract(T resource,long amount,TransactionContext transaction,CallbackInfoReturnable<Long> result) {
        if(this.offroad$policy!=null&&!this.offroad$policy.permitsExtraction())result.setReturnValue(0L);
    }
    @Inject(method="extract",at=@At("RETURN"),require=1)
    private void offroad$committedExtraction(T resource,long amount,TransactionContext transaction,CallbackInfoReturnable<Long> result) {
        if(this.offroad$policy!=null)this.offroad$policy.extracted(result.getReturnValue(),transaction);
    }
    @Inject(method="supportsInsertion",at=@At("HEAD"),cancellable=true,require=1)
    private void offroad$supportsInsertion(CallbackInfoReturnable<Boolean> result) {
        if(this.offroad$policy!=null&&!this.offroad$policy.permitsInsertion())result.setReturnValue(false);
    }
    @Inject(method="supportsExtraction",at=@At("HEAD"),cancellable=true,require=1)
    private void offroad$supportsExtraction(CallbackInfoReturnable<Boolean> result) {
        if(this.offroad$policy!=null&&!this.offroad$policy.permitsExtraction())result.setReturnValue(false);
    }
    @Inject(method="guard",at=@At("RETURN"),cancellable=true,require=1)
    private void offroad$guardView(StorageView<T> source,CallbackInfoReturnable<StorageView<T>> result) {
        if(this.offroad$policy!=null)result.setReturnValue(this.offroad$policy.view(result.getReturnValue()));
    }
    @Inject(method="guardSlot",at=@At("RETURN"),cancellable=true,require=1)
    private void offroad$guardSlot(SingleSlotStorage<T> source,CallbackInfoReturnable<SingleSlotStorage<T>> result) {
        if(this.offroad$policy!=null)result.setReturnValue(this.offroad$policy.slot(result.getReturnValue()));
    }
}
