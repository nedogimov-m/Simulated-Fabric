package dev.simulated_team.simulated.content.blocks.docking_connector;

import dev.simulated_team.simulated.multiloader.energy.SingleBattery;
import dev.simulated_team.simulated.service.SimInventoryService;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;

public class DockingConnectorBattery extends SingleBattery implements SimInventoryService.RoutedBattery {
    private DockingConnectorBattery other = null;
    private final Runnable committed;

    public DockingConnectorBattery(final int maxEnergy, final int throughput, final Runnable committed) {
        super(maxEnergy, throughput);
        this.committed = Objects.requireNonNull(committed);
    }

    public void connect(final DockingConnectorBattery other) {
        this.other = other;
    }

    public void disconnect() {
        this.other = null;
    }

    @Override
    public @Nullable SingleBattery insertionBattery() {
        return this.other;
    }

    @Override
    public void onBatteryCommit() {
        this.committed.run();
    }

    protected int superReceiveEnergy(final int toReceive, final boolean simulate) {
        return super.receiveEnergy(toReceive, simulate);
    }

    @Override
    public int receiveEnergy(final int toReceive, final boolean simulate) {
        if (this.other != null) {
            return this.other.superReceiveEnergy(toReceive, simulate);
        }
        return 0;
    }

    @Override
    public boolean canReceive() {
        return this.other != null;
    }
}
