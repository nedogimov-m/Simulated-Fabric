package dev.simulated_team.simulated.fabric.storage;

import dev.simulated_team.simulated.multiloader.tanks.CFluidType;
import dev.simulated_team.simulated.multiloader.tanks.SingleTank;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;

import java.util.Objects;

/** Native Fabric fluid storage; capacity and amounts use Fabric fluid units. */
public final class FabricSingleTankStorage extends SnapshotParticipant<SingleTank.Snapshot> implements SingleSlotStorage<FluidVariant> {
    private final SingleTank tank;
    private final boolean physical;
    private final Runnable changed;

    public FabricSingleTankStorage(SingleTank tank) { this(tank, false, () -> {}); }

    private FabricSingleTankStorage(SingleTank tank, boolean physical, Runnable changed) {
        this.tank = Objects.requireNonNull(tank);
        this.physical = physical;
        this.changed = Objects.requireNonNull(changed);
    }

    /** Normal polymorphic storage with a final-commit persistence callback. */
    public static FabricSingleTankStorage withCommitCallback(SingleTank tank, Runnable changed) {
        return new FabricSingleTankStorage(tank, false, changed);
    }

    /** A resolved physical target must bypass its facade's virtual forwarding methods. */
    public static FabricSingleTankStorage forPhysicalTank(SingleTank tank, Runnable changed) {
        return new FabricSingleTankStorage(tank, true, changed);
    }

    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);
        if (!this.physical) return this.tank.insert(type(resource), maxAmount, false, () -> this.updateSnapshots(transaction));
        final long accepted = SingleTank.calculateInsert(this.tank, type(resource), maxAmount);
        if (accepted > 0) {
            this.updateSnapshots(transaction);
            SingleTank.applyInsert(this.tank, type(resource), accepted);
        }
        return accepted;
    }

    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);
        if (!this.physical) return this.tank.extract(type(resource), maxAmount, false, () -> this.updateSnapshots(transaction));
        final long extracted = SingleTank.calculateExtract(this.tank, type(resource), maxAmount);
        if (extracted > 0) {
            this.updateSnapshots(transaction);
            SingleTank.applyExtract(this.tank, extracted);
        }
        return extracted;
    }

    @Override
    public boolean isResourceBlank() { return this.tank.type.isBlank(); }

    @Override
    public FluidVariant getResource() { return FluidVariant.of(this.tank.type.fluid(), this.tank.type.data()); }

    @Override
    public long getAmount() { return this.tank.amount; }

    @Override
    public long getCapacity() { return this.tank.capacity; }

    @Override
    protected SingleTank.Snapshot createSnapshot() {
        return this.physical ? new SingleTank.Snapshot(this.tank.type, this.tank.amount) : this.tank.createSnapshot();
    }

    @Override
    protected void readSnapshot(SingleTank.Snapshot snapshot) {
        if (this.physical) {
            this.tank.type = snapshot.type();
            this.tank.amount = snapshot.amount();
        } else {
            this.tank.readSnapshot(snapshot.type(), snapshot.amount());
        }
    }

    @Override
    protected void onFinalCommit() { this.changed.run(); }

    private static CFluidType type(FluidVariant variant) {
        return new CFluidType(variant.getFluid(), variant.getComponentsPatch());
    }
}
