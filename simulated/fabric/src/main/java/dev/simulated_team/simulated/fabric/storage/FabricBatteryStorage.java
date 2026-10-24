package dev.simulated_team.simulated.fabric.storage;

import dev.simulated_team.simulated.multiloader.energy.SingleBattery;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import team.reborn.energy.api.EnergyStorage;

import java.util.Objects;

/** Real Team Reborn Energy API integration, with physical-owner snapshots and commit-only persistence. */
public final class FabricBatteryStorage extends SnapshotParticipant<Integer> implements EnergyStorage {
    private final SingleBattery battery;
    private final Runnable changed;
    private final boolean physical;

    public FabricBatteryStorage(SingleBattery battery, Runnable changed, boolean physical) {
        this.battery = Objects.requireNonNull(battery);
        this.changed = Objects.requireNonNull(changed);
        this.physical = physical;
    }

    @Override public boolean supportsInsertion() { return this.battery.canReceive(); }
    @Override public boolean supportsExtraction() { return this.battery.canExtract(); }
    @Override public long getAmount() { return this.battery.getEnergy(); }
    @Override public long getCapacity() { return this.battery.maxEnergy; }

    @Override
    public long insert(long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notNegative(maxAmount);
        if (!this.physical && !this.battery.canReceive()) return 0;
        final int requested = (int) Math.min(maxAmount, Integer.MAX_VALUE);
        final int accepted = this.physical ? Math.max(0, Math.min(Math.min(requested,
                this.battery.maxEnergy - this.battery.getEnergy()), this.battery.getThroughput())) : this.battery.receiveEnergy(requested, true);
        if (accepted > 0) {
            this.updateSnapshots(transaction);
            if (this.physical) this.battery.setEnergy(this.battery.getEnergy() + accepted);
            else this.battery.receiveEnergy(accepted, false);
        }
        return accepted;
    }

    @Override
    public long extract(long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notNegative(maxAmount);
        if (!this.battery.canExtract()) return 0;
        final int requested = (int) Math.min(maxAmount, Integer.MAX_VALUE);
        final int extracted = this.physical ? Math.max(0, Math.min(Math.min(requested,
                this.battery.getEnergy()), this.battery.getThroughput())) : this.battery.extractEnergy(requested, true);
        if (extracted > 0) {
            this.updateSnapshots(transaction);
            if (this.physical) this.battery.setEnergy(this.battery.getEnergy() - extracted);
            else this.battery.extractEnergy(extracted, false);
        }
        return extracted;
    }

    @Override protected Integer createSnapshot() { return this.battery.getEnergy(); }
    @Override protected void readSnapshot(Integer energy) { this.battery.setEnergy(energy); }
    @Override protected void onFinalCommit() { this.changed.run(); }
}
