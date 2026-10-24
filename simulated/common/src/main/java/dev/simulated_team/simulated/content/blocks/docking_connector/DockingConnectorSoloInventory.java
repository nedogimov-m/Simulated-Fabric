package dev.simulated_team.simulated.content.blocks.docking_connector;

import dev.simulated_team.simulated.multiloader.inventory.ItemInfoWrapper;
import dev.simulated_team.simulated.multiloader.inventory.SingleSlotContainer;
import java.util.Objects;

public class DockingConnectorSoloInventory extends SingleSlotContainer {
    private boolean allowsInsertion = false;
    private final Runnable changed;
    public DockingConnectorSoloInventory(final Runnable changed) {
        super(64);
        this.changed = Objects.requireNonNull(changed);
    }

    @Override
    public void setChanged() {
        this.changed.run();
    }

    public void dock() {
        this.allowsInsertion = true;
    }

    public void unDock() {
        this.allowsInsertion = false;
    }

    @Override
    public boolean canInsertItem(final ItemInfoWrapper info) {
        return this.allowsInsertion;
    }
}
