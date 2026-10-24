package dev.simulated_team.simulated.fabric.registry;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.index.SimStats;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;

/** Registers the real custom statistics and their menu formatters during Fabric common initialization. */
public final class NativeSimStats extends SimStats {
    private static boolean registered;

    public static void register() {
        if (registered) return;
        new NativeSimStats().init();
        registered = true;
    }

    @Override
    protected SimStats.Stat makeCustomStat(final String key, final StatFormatter formatter) {
        final Identifier id = Simulated.path(key);
        final Identifier registeredId = Registry.register(BuiltInRegistries.CUSTOM_STAT, id, id);
        // Populate Stats.CUSTOM now, so the menu knows the original formatter before the first award.
        Stats.CUSTOM.get(registeredId, formatter);
        return new SimStats.Stat(() -> registeredId, formatter);
    }

    private NativeSimStats() {}
}
