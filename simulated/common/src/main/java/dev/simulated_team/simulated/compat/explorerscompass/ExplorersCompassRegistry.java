package dev.simulated_team.simulated.compat.explorerscompass;

import com.chaosthedude.explorerscompass.ExplorersCompass;
import dev.simulated_team.simulated.index.SimNavigationTargets;

public class ExplorersCompassRegistry {
    public static void init() {
        SimNavigationTargets.register("explorers_compass", ExplorersCompassNavigationTarget::new, () -> ExplorersCompass.EXPLORERS_COMPASS_ITEM);
    }
}
