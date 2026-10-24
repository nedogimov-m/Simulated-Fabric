package dev.simulated_team.simulated.content.blocks.rope.strand.server;

import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorRidingState;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.service.SimConfigService;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import org.joml.Vector3dc;

/**
 * Contains and manages all ropes for a client level
 */
public class ServerLevelRopeManager {
    private static final AttachmentType<ServerLevelRopeManager> LEVEL_ROPES = AttachmentRegistry.create(Simulated.path("server_rope_manager"));

    private final Level level;
    private final Map<UUID, ServerRopeStrand> ropeStrands = new Object2ObjectOpenHashMap<>();
    private final Map<UUID, Ride> riders = new Object2ObjectOpenHashMap<>();
    private record Ride(UUID strand, long expiresAt) { }

    public ServerLevelRopeManager(final Level level) {
        this.level = level;
    }

    @Nullable
    public static ServerLevelRopeManager getOrCreate(final Level level) {
        if (!(level instanceof final ServerLevel serverLevel)) return null;
        return ((AttachmentTarget) serverLevel).getAttachedOrCreate(LEVEL_ROPES, () -> new ServerLevelRopeManager(serverLevel));
    }

    public void addStrand(final ServerRopeStrand strand) {
        this.ropeStrands.put(strand.getUUID(), strand);
    }

    @Nullable
    public ServerRopeStrand getStrand(final UUID uuid) {
        return this.ropeStrands.get(uuid);
    }

    public void removeStrand(final UUID uuid) {
        this.ropeStrands.remove(uuid);
    }

    public Collection<ServerRopeStrand> getAllStrands() {
        return this.ropeStrands.values();
    }

    /** Rope sessions are separate from Create's conveyor sessions and expire without a client heartbeat. */
    public void ridingHeartbeat(final ServerPlayer player, final UUID strand, final boolean stop) {
        if (stop) {
            final Ride riding = this.riders.get(player.getUUID());
            if (riding != null && riding.strand().equals(strand)) this.riders.remove(player.getUUID());
            return;
        }
        if (!this.canRide(player, strand)) return;
        this.riders.put(player.getUUID(), new Ride(strand, this.level.getGameTime() + 20));
        resetRidingFall(player);
    }

    public void tickRiders() {
        final var iterator = this.riders.entrySet().iterator();
        while (iterator.hasNext()) {
            final var entry = iterator.next();
            final var player = this.level.getPlayerByUUID(entry.getKey());
            if (!(player instanceof final ServerPlayer serverPlayer) || serverPlayer.isRemoved()
                    || entry.getValue().expiresAt() < this.level.getGameTime()
                    || !this.canRide(serverPlayer, entry.getValue().strand())) {
                iterator.remove();
            } else {
                resetRidingFall(serverPlayer);
            }
        }
    }

    private static void resetRidingFall(final ServerPlayer player) {
        player.connection.resetFlyingTicks();
        player.fallDistance = 0;
    }

    private boolean canRide(final ServerPlayer player, final UUID strandId) {
        final var strand = this.ropeStrands.get(strandId);
        if (player.level() != this.level || !player.isAlive() || player.isShiftKeyDown() || player.getAbilities().flying
                || strand == null || !strand.isActive() || !player.getMainHandItem().is(ChainConveyorRidingState.CHAIN_RIDEABLE)) {
            return false;
        }
        final var hangingPosition = player.position().add(0, player.getBoundingBox().getYsize() + 0.5 * player.getScale(), 0);
        final double reach = player.blockInteractionRange() + 1;
        final double slopeLimit = Math.sin(Math.toRadians(SimConfigService.INSTANCE.server().blocks.maxRopeZiplineAngle.getF()));
        final var points = strand.getPoints();
        double nearestDistanceSquared = Double.POSITIVE_INFINITY;
        double nearestSlope = Double.POSITIVE_INFINITY;
        for (int i = 1; i < points.size(); i++) {
            final Vector3dc start = points.get(i - 1);
            final Vector3dc end = points.get(i);
            final double x = end.x() - start.x(), y = end.y() - start.y(), z = end.z() - start.z();
            final double lengthSquared = x * x + y * y + z * z;
            if (lengthSquared < 1e-12) continue;
            final double along = Math.clamp(((hangingPosition.x - start.x()) * x + (hangingPosition.y - start.y()) * y
                    + (hangingPosition.z - start.z()) * z) / lengthSquared, 0, 1);
            final double distanceSquared = hangingPosition.distanceToSqr(start.x() + along * x, start.y() + along * y, start.z() + along * z);
            if (distanceSquared < nearestDistanceSquared) {
                nearestDistanceSquared = distanceSquared;
                nearestSlope = Math.abs(y) / Math.sqrt(lengthSquared);
            }
        }
        return nearestDistanceSquared <= reach * reach && nearestSlope <= slopeLimit;
    }

    public void physicsTick(final SubLevelPhysicsSystem physicsSystem, final double timeStep) {
        final ServerLevel level = physicsSystem.getLevel();

        for (final ServerRopeStrand strand : this.ropeStrands.values()) {
            if (!strand.isActive()) {
                continue;
            }

            if (!strand.isOwnerLoaded(level) || !strand.areAttachmentsLoaded(level)) {
                physicsSystem.removeObject(strand);
                continue;
            }

            strand.prePhysicsTick(physicsSystem, level, timeStep);
        }
    }
}
