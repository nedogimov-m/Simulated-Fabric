package dev.simulated_team.simulated.service;

import com.simibubi.create.content.contraptions.MountedStorageManager;
import java.util.function.Consumer;
import dev.simulated_team.simulated.multiloader.energy.SingleBattery;
import dev.simulated_team.simulated.multiloader.inventory.AbstractContainer;
import dev.simulated_team.simulated.multiloader.inventory.InventoryLoaderWrapper;
import dev.simulated_team.simulated.multiloader.tanks.SingleTank;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

public interface SimInventoryService {

	SimInventoryService INSTANCE = ServiceUtil.load(SimInventoryService.class);

	@Nullable InventoryLoaderWrapper getInventory(@Nullable final BlockEntity be, @Nullable final Direction dir);

	//I don't think there's an easier way to do this...
	InventoryLoaderWrapper getWrappedAllItemsFromContraption(final MountedStorageManager manager);
	InventoryLoaderWrapper getWrappedMountedItemsFromContraption(final MountedStorageManager manager);

	<T extends BlockEntity> Consumer<BlockEntityType<T>> registerInventory(final BiFunction<T, Direction, AbstractContainer> getter);

	<T extends BlockEntity> Consumer<BlockEntityType<T>> registerTank(final BiFunction<T, Direction, SingleTank> getter);

	<T extends BlockEntity> Consumer<BlockEntityType<T>> registerBattery(final BiFunction<T, Direction, SingleBattery> getter);

    /** A forwarding tank exposes its current physical insertion target for native transaction ownership. */
    interface RoutedTank {
        @Nullable SingleTank insertionTank();
        void onTankCommit();
    }

    /** A forwarding battery exposes its current physical insertion target for native transaction ownership. */
    interface RoutedBattery {
        @Nullable SingleBattery insertionBattery();
        void onBatteryCommit();
    }
}
