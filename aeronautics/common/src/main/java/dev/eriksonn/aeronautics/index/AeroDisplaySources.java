package dev.eriksonn.aeronautics.index;

import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import dev.eriksonn.aeronautics.content.display_sources.GasDisplaySource;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;

public final class AeroDisplaySources {
    public static final GasDisplaySource GAS_DISPLAY = Registry.register(CreateBuiltInRegistries.DISPLAY_SOURCE,
            Identifier.fromNamespaceAndPath("aeronautics", "gas_display"), new GasDisplaySource());
    private AeroDisplaySources() {}
}
