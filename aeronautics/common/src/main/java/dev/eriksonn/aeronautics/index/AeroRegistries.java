package dev.eriksonn.aeronautics.index;

import dev.eriksonn.aeronautics.api.levitite_blend_crystallization.CrystalPropagationContext;
import dev.eriksonn.aeronautics.content.blocks.hot_air.lifting_gas.LiftingGasType;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public class AeroRegistries {
    public static class Keys {
        public static final ResourceKey<Registry<LiftingGasType>> LIFTING_GAS_TYPE = key("lifting_gas_type");
        public static final ResourceKey<Registry<CrystalPropagationContext>> LEVITITE_CRYSTAL_PROPAGATION_CONTEXT = key("levitite_crystal_propagation_context");

        private static <T> ResourceKey<Registry<T>> key(final String name) {
            return ResourceKey.createRegistryKey(net.minecraft.resources.Identifier.fromNamespaceAndPath("aeronautics", name));
        }
    }

    public static final Registry<LiftingGasType> LIFTING_GAS_TYPE = registry(AeroRegistries.Keys.LIFTING_GAS_TYPE);
    public static final Registry<CrystalPropagationContext> LEVITITE_CRYSTAL_PROPAGATION_CONTEXT = registry(Keys.LEVITITE_CRYSTAL_PROPAGATION_CONTEXT);

    private static <T> Registry<T> registry(final ResourceKey<Registry<T>> registryKey) {
        return FabricRegistryBuilder.create(registryKey).buildAndRegister();
    }

    public static void init() {
        // no-op, for JIT
    }
}
