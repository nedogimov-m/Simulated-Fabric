package dev.ryanhcode.offroad.fabric.service;

import dev.ryanhcode.offroad.config.OffroadConfig;
import dev.ryanhcode.offroad.config.server.OffroadServer;
import dev.ryanhcode.offroad.config.client.OffroadClientConfig;
import dev.simulated_team.simulated.config.AddonConfigManager;
import dev.simulated_team.simulated.fabric.config.FabricAddonConfigLifecycle;

public final class FabricOffroadConfigService implements OffroadConfig {
    private static final OffroadServer SERVER = new OffroadServer();
    private static final OffroadClientConfig CLIENT = new OffroadClientConfig();
    private static final AddonConfigManager<OffroadServer, OffroadClientConfig> MANAGER =
            new AddonConfigManager<>("offroad", SERVER, CLIENT, SERVER.kinetics.stressValues);

    public static AddonConfigManager<OffroadServer, OffroadClientConfig> manager() { return MANAGER; }
    public static void register() { FabricAddonConfigLifecycle.register(MANAGER); }
    @Override public OffroadServer getServerConfig() { return SERVER; }
    @Override public OffroadClientConfig getClientConfig() { return CLIENT; }
}
