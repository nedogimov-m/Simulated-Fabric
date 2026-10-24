package dev.eriksonn.aeronautics.index;

import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.content.blocks.hot_air.lifting_gas.DefaultLiftingGas;
import dev.eriksonn.aeronautics.content.blocks.hot_air.lifting_gas.SteamLiftingGas;
import net.minecraft.core.Registry;

public final class AeroLiftingGasTypes {
    public static final DefaultLiftingGas DEFAULT_GAS = Registry.register(AeroRegistries.LIFTING_GAS_TYPE,
            Aeronautics.path("default"), new DefaultLiftingGas());
    public static final SteamLiftingGas STEAM = Registry.register(AeroRegistries.LIFTING_GAS_TYPE,
            Aeronautics.path("steam"), new SteamLiftingGas());
    public static void init() {}
    private AeroLiftingGasTypes() {}
}
