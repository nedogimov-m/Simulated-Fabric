package dev.simulated_team.simulated.content.navigation_targets.lodestone_compass_compatability;

import dev.simulated_team.simulated.fabric.registry.NativeLevelData;
import dev.simulated_team.simulated.Simulated;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.joml.Vector3d;

import java.util.UUID;

public class ClientLodestonePositions {

	public static final NativeLevelData<ClientLodestonePositions> clientPositions = new NativeLevelData<>(Simulated.path("client_lodestone_positions"), level -> new ClientLodestonePositions());
	public final Object2ObjectOpenHashMap<UUID, Vector3d> CLIENT_LODESTONE_MAP = new Object2ObjectOpenHashMap<>();


}
