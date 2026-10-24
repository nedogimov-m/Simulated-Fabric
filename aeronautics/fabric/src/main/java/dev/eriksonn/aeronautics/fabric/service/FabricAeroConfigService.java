package dev.eriksonn.aeronautics.fabric.service;

import dev.eriksonn.aeronautics.config.AeroConfig;
import dev.eriksonn.aeronautics.config.server.AeroServer;
import dev.eriksonn.aeronautics.config.client.AeroClient;
import dev.simulated_team.simulated.config.AddonConfigManager;
import dev.simulated_team.simulated.fabric.config.FabricAddonConfigLifecycle;

public final class FabricAeroConfigService implements AeroConfig {
    private static final AeroServer SERVER = new AeroServer();
    private static final AeroClient CLIENT = new AeroClient();
    private static final AddonConfigManager<AeroServer, AeroClient> MANAGER =
            new AddonConfigManager<>("aeronautics", SERVER, CLIENT, SERVER.kinetics.stressValues);

    public static AddonConfigManager<AeroServer, AeroClient> manager() { return MANAGER; }
    public static void register() { FabricAddonConfigLifecycle.register(MANAGER); }
    @Override public AeroServer getServerConfig() { return SERVER; }
    @Override public AeroClient getClientConfig() { return CLIENT; }
}
