package dev.simulated_team.simulated.index;

import com.mojang.serialization.Lifecycle;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.client.BlockPropertiesTooltip;
import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.NavigationTarget;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public class SimRegistries {
	public static class Keys {
		public static final ResourceKey<Registry<NavigationTarget>> NAVIGATION_TARGET = key("navigation_target");
		public static final ResourceKey<Registry<BlockPropertiesTooltip.Entry>> PROPERTY_TOOLTIP = key("property_tooltip");

		private static <T> ResourceKey<Registry<T>> key(final String name) {
			return ResourceKey.createRegistryKey(Simulated.path(name));
		}
	}

	public static final Registry<NavigationTarget> NAVIGATION_TARGET = registry(Keys.NAVIGATION_TARGET);
	// Tooltip functions are registered by the client and never serialized or synchronized.
	// Keep their public Registry API without adding an empty server registry to BuiltInRegistries.
	public static final Registry<BlockPropertiesTooltip.Entry> PROPERTY_TOOLTIP =
			new MappedRegistry<>(Keys.PROPERTY_TOOLTIP, Lifecycle.stable());

	private static <T> Registry<T> registry(final ResourceKey<Registry<T>> registryKey) {
		return FabricRegistryBuilder.create(registryKey).buildAndRegister();
	}

	public static void register() {}
}
