package dev.simulated_team.simulated.content.blocks.handle;

import dev.simulated_team.simulated.index.SimStats;
import dev.simulated_team.simulated.network.packets.handle.ClientboundPlayersHoldingHandlePacket;
import foundry.veil.api.network.VeilPacketManager;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/** Each server owns its handle heartbeats; TTL expiry and broadcasts run on its native tick. */
public class ServerHandleHoldingHandler {
    private static final Map<MinecraftServer, State> SERVERS = new WeakHashMap<>();
    private static final class State {
        final Object2IntOpenHashMap<UUID> players = new Object2IntOpenHashMap<>();
        int ticks;
    }
    private static State state(MinecraftServer server) { return SERVERS.computeIfAbsent(server, ignored -> new State()); }
    public static void startHolding(Player player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        State state = state(level.getServer()); int before = state.players.size();
        if (state.players.put(player.getUUID(), 20) <= 0) SimStats.INTERACT_WITH_HANDLE.awardTo(player);
        if (state.players.size() != before) sync(level.getServer(), state);
    }
    public static void stopHolding(Player player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        State state = state(level.getServer());
        if (state.players.removeInt(player.getUUID()) != 0) sync(level.getServer(), state);
    }
    public static void tick(MinecraftServer server) {
        State state = state(server); state.ticks++;
        int before = state.players.size(); var iterator = state.players.object2IntEntrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next(); int remaining = entry.getIntValue() - 1;
            if (remaining <= 0 || server.getPlayerList().getPlayer(entry.getKey()) == null) iterator.remove();
            else entry.setValue(remaining);
        }
        if (state.ticks % 10 == 0 || before != state.players.size()) sync(server, state);
    }
    private static void sync(MinecraftServer server, State state) {
        VeilPacketManager.all(server).sendPacket(new ClientboundPlayersHoldingHandlePacket(Set.copyOf(state.players.keySet())));
    }
    public static void unload(MinecraftServer server) { SERVERS.remove(server); }
}
