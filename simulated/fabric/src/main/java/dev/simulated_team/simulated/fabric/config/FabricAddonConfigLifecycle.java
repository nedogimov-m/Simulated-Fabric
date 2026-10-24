package dev.simulated_team.simulated.fabric.config;

import com.simibubi.create.content.kinetics.KineticNetworks;
import dev.simulated_team.simulated.config.AddonConfigManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Shared Fabric lifecycle for the three real addon scope managers. */
public final class FabricAddonConfigLifecycle {
    private static final Map<String, AddonConfigManager<?, ?>> CONFIGS = new LinkedHashMap<>();
    private record Pending(MinecraftServer owner, Runnable operation) {}
    private static final ConcurrentLinkedQueue<Pending> PENDING = new ConcurrentLinkedQueue<>();
    private static volatile MinecraftServer activeServer;
    private static boolean registered;

    public static synchronized void register(AddonConfigManager<?, ?> manager) {
        final var existing = CONFIGS.get(manager.modId());
        if (existing != null) {
            if (existing != manager) throw new IllegalStateException("Duplicate addon config owner " + manager.modId());
            return;
        }
        manager.configure(FabricAddonConfigLifecycle::isServerThread,
                operation -> writeServer(manager, operation), ignored -> serverChanged(manager));
        manager.initialize(FabricLoader.getInstance().getConfigDir(),
                FabricLoader.getInstance().getGameDir().resolve("defaultconfigs"),
                FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT);
        CONFIGS.put(manager.modId(), manager);
        if (registered) return;
        PayloadTypeRegistry.clientboundPlay().register(AddonConfigSyncPayload.TYPE, AddonConfigSyncPayload.CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!ServerPlayNetworking.canSend(handler.getPlayer(), AddonConfigSyncPayload.TYPE)) return;
            for (var config : configurations()) sender.sendPacket(packet(config));
        });
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            activeServer = server;
            for (var config : configurations()) config.startWorld(server.getWorldPath(LevelResource.ROOT).resolve("serverconfig"));
        });
        ServerTickEvents.END_SERVER_TICK.register(FabricAddonConfigLifecycle::drain);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> {
            if (success) for (var config : configurations()) config.reloadServer();
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(FabricAddonConfigLifecycle::drain);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            if (activeServer != server) return;
            PENDING.removeIf(pending -> pending.owner == server);
            activeServer = null;
            for (var config : configurations()) config.stopWorld();
        });
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) FabricAddonClientConfigLifecycle.register();
        registered = true;
    }

    static synchronized List<AddonConfigManager<?, ?>> configurations() { return List.copyOf(CONFIGS.values()); }
    private static boolean isServerThread() { var server = activeServer; return server != null && server.isSameThread(); }

    private static void writeServer(AddonConfigManager<?, ?> manager, Runnable operation) {
        var server = activeServer;
        if (server == null) {
            if (manager.serverStore().hasRemote()) throw new IllegalStateException("Remote server configuration is read-only");
            operation.run();
        } else if (server.isSameThread() && !Transaction.isOpen()) {
            operation.run();
        } else {
            PENDING.add(new Pending(server, operation));
        }
    }

    private static void drain(MinecraftServer server) {
        if (activeServer != server || Transaction.isOpen()) return;
        for (int remaining = PENDING.size(); remaining > 0; remaining--) {
            final var pending = PENDING.poll();
            if (pending == null) break;
            if (pending.owner == server) pending.operation.run();
        }
    }

    private static AddonConfigSyncPayload packet(AddonConfigManager<?, ?> manager) {
        return new AddonConfigSyncPayload(manager.modId(), manager.serverStore().serverDocument().toString());
    }

    private static void serverChanged(AddonConfigManager<?, ?> manager) {
        var server = activeServer;
        if (server == null) return;
        for (var level : server.getAllLevels()) KineticNetworks.refreshStressProviders(level);
        var players = server.getPlayerList();
        if (players == null) return;
        for (var player : players.getPlayers()) {
            if (ServerPlayNetworking.canSend(player, AddonConfigSyncPayload.TYPE)) ServerPlayNetworking.send(player, packet(manager));
        }
    }

    private FabricAddonConfigLifecycle() {}
}
