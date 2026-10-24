package dev.ryanhcode.offroad.config.server;

import dev.simulated_team.simulated.config.AddonStressConfig;
import java.util.LinkedHashMap;
import java.util.Map;

public final class OffroadStress extends AddonStressConfig {
    public OffroadStress() {
        super("offroad", impacts(), capacities());
    }

    private static Map<String, Double> impacts() {
        final Map<String, Double> values = new LinkedHashMap<>();
        values.put("borehead_bearing", 8.0);
        values.put("wheel_mount", 16.0);
        return values;
    }
    private static Map<String, Double> capacities() {
        final Map<String, Double> values = new LinkedHashMap<>();
        return values;
    }
}
