package dev.simulated_team.simulated.index;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.NavigationTarget;
import dev.simulated_team.simulated.content.navigation_targets.*;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class SimNavigationTargets {
    private static final Map<NavigationTarget, Supplier<? extends ItemLike>> ITEMS = new LinkedHashMap<>();
    public static final CompassNavigationTarget COMPASS = register("compass", CompassNavigationTarget::new, () -> Items.COMPASS);
    public static final RecoveryCompassNavigationTarget RECOVERY_COMPASS = register("recovery_compass", RecoveryCompassNavigationTarget::new, () -> Items.RECOVERY_COMPASS);
    public static final MapNavigationTarget MAP = register("map", MapNavigationTarget::new, () -> Items.FILLED_MAP);
    public static final MagnetNavigationTarget MAGNET = register("magnet", MagnetNavigationTarget::new, () -> SimBlocks.REDSTONE_MAGNET);

    public static <T extends NavigationTarget> T register(String name, Supplier<T> factory, Supplier<? extends ItemLike> item) {
        final T target = Registry.register(SimRegistries.NAVIGATION_TARGET, Simulated.path(name), factory.get());
        ITEMS.put(target, item);
        return target;
    }

    public static void onAddDefaultComponents(BiConsumer<ItemLike, Consumer<DataComponentMap.Builder>> modify) {
        ITEMS.forEach((target, item) -> modify.accept(item.get(), builder -> builder.set(SimDataComponents.TARGET, target)));
    }

    public static void register() {}
    private SimNavigationTargets() {}
}
