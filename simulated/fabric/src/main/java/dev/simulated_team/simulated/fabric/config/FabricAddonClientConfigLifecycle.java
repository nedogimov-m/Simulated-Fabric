package dev.simulated_team.simulated.fabric.config;

import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Loaded only on Fabric clients; server values never overwrite the client's local files. */
final class FabricAddonClientConfigLifecycle {
    static void register() {
        ClientPlayNetworking.registerGlobalReceiver(AddonConfigSyncPayload.TYPE, (packet, context) -> context.client().execute(() -> {
            for (var manager : FabricAddonConfigLifecycle.configurations()) {
                if (!manager.modId().equals(packet.modId())) continue;
                try {
                    manager.installRemote(JsonParser.parseString(packet.document()).getAsJsonObject());
                } catch (IllegalArgumentException failure) {
                    LogUtils.getLogger().warn("Rejected invalid {} server configuration", packet.modId(), failure);
                }
            }
        }));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            for (var manager : FabricAddonConfigLifecycle.configurations()) manager.clearRemote();
        });
    }

    private FabricAddonClientConfigLifecycle() {}
}
