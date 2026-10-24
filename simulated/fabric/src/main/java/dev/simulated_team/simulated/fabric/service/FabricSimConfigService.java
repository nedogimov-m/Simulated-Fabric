package dev.simulated_team.simulated.fabric.service;

import dev.simulated_team.simulated.service.SimConfigService;
import dev.simulated_team.simulated.config.server.SimServer;
import dev.simulated_team.simulated.config.client.SimClient;
import dev.simulated_team.simulated.config.AddonConfigManager;
import dev.simulated_team.simulated.fabric.config.FabricAddonConfigLifecycle;

public final class FabricSimConfigService implements SimConfigService {
    private static final SimServer SERVER = new SimServer();
    private static final SimClient CLIENT = new SimClient();
    private static final AddonConfigManager<SimServer, SimClient> MANAGER =
            new AddonConfigManager<>("simulated", SERVER, CLIENT, SERVER.kinetics.stressValues);

    public static AddonConfigManager<SimServer, SimClient> manager() { return MANAGER; }
    public static void register() { FabricAddonConfigLifecycle.register(MANAGER); }
    @Override public SimServer server() { return SERVER; }
    @Override public SimClient client() { return CLIENT; }
    @Override public boolean serverLoaded() { return MANAGER.serverLoaded(); }
    @Override public boolean clientLoaded() { return MANAGER.clientLoaded(); }
}
