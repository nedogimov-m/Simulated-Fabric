package dev.simulated_team.simulated.compat.naturescompass;

import com.chaosthedude.naturescompass.NaturesCompass;
import dev.simulated_team.simulated.index.SimNavigationTargets;

public class NaturesCompassRegistry {
    public static void init() {
        SimNavigationTargets.register("natures_compass", NaturesCompassNavigationTarget::new, () -> NaturesCompass.NATURES_COMPASS_ITEM);
    }
}
