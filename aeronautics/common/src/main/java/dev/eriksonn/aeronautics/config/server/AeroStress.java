package dev.eriksonn.aeronautics.config.server;

import dev.simulated_team.simulated.config.AddonStressConfig;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AeroStress extends AddonStressConfig {
    public AeroStress() {
        super("aeronautics", impacts(), capacities());
    }

    private static Map<String, Double> impacts() {
        final Map<String, Double> values = new LinkedHashMap<>();
        values.put("propeller_bearing", 2.0);
        values.put("gyroscopic_propeller_bearing", 2.0);
        values.put("smart_propeller", 4.0);
        values.put("andesite_propeller", 4.0);
        values.put("wooden_propeller", 4.0);
        values.put("mounted_potato_cannon", 2.0);
        return values;
    }
    private static Map<String, Double> capacities() {
        final Map<String, Double> values = new LinkedHashMap<>();
        return values;
    }
}
