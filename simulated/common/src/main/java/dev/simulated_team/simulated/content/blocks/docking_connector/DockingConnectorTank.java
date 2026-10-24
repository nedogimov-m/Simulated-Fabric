package dev.simulated_team.simulated.content.blocks.docking_connector;

import dev.simulated_team.simulated.multiloader.tanks.CFluidType;
import dev.simulated_team.simulated.multiloader.tanks.SingleTank;
import dev.simulated_team.simulated.service.SimInventoryService;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class DockingConnectorTank extends SingleTank implements SimInventoryService.RoutedTank {

    private final DockingConnectorBlockEntity blockEntity;
    private BlockPos connectedPos;
    private DockingConnectorTank connectedTank;

    public DockingConnectorTank(final DockingConnectorBlockEntity blockEntity) {
        super(1000);
        this.blockEntity = blockEntity;
    }

    public void connect(final BlockPos pos, final DockingConnectorTank other) {
        this.connectedPos = pos;
        this.connectedTank = other;
    }

    public void disconnect() {
        this.connectedPos = null;
        this.connectedTank = null;
    }

    private boolean canInteract() {
        final Level level = this.blockEntity.getLevel();
        if (level == null) {
            return false;
        }

        if (this.connectedPos == null || !(level.getBlockEntity(this.connectedPos) instanceof final DockingConnectorBlockEntity other)) {
            return false;
        }

        return this.connectedTank == other.tank;
    }

    @Override
    public @Nullable SingleTank insertionTank() {
        return this.canInteract() ? this.connectedTank : null;
    }

    @Override
    public void onTankCommit() {
        this.blockEntity.notifyUpdate();
    }

    @Override
    public long insert(final CFluidType insertedType, final long maxAmount, final boolean simulate, final Runnable beforeApply) {
        if (!this.canInteract()) {
            return 0;
        }

        final long v = calculateInsert(this.connectedTank, insertedType, maxAmount);
        if (!simulate && v > 0) {
            if (beforeApply != null) {
                beforeApply.run();
            }
            applyInsert(this.connectedTank, insertedType, v);
        }
        return v;
    }

}
