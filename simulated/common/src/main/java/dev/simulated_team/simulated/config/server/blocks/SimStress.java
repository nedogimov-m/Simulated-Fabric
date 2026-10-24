package dev.simulated_team.simulated.config.server.blocks;

import dev.simulated_team.simulated.config.AddonStressConfig;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.item.DyeColor;

public final class SimStress extends AddonStressConfig {
    public SimStress() {
        super("simulated", impacts(), capacities());
    }

    private static Map<String, Double> impacts() {
        final Map<String, Double> values = new LinkedHashMap<>();
        values.put("swivel_bearing", 4.0);
        values.put("rope_winch", 4.0);
        values.put("torsion_spring", 16.0);
        values.put("auger_shaft", 0.5);
        values.put("auger_cog", 0.5);
        return values;
    }
    private static Map<String, Double> capacities() {
        final Map<String, Double> values = new LinkedHashMap<>();
        values.put("torsion_spring", 8.0);
        values.put("steering_wheel", 16.0);
        for (DyeColor color : DyeColor.values()) {
            values.put(color.getSerializedName() + "_portable_engine", 64.0);
        }
        return values;
    }
}
