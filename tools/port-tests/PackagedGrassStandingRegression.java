import com.google.gson.GsonBuilder;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.joml.Vector3d;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/** A free grass body resting on real terrain; actual SDL native player input only. */
public final class PackagedGrassStandingRegression {
    private static final BlockPos SOURCE = new BlockPos(0, 121, 0);
    private static final Path PROOF = Path.of(System.getProperty("port.smoke.report"))
            .resolveSibling("grass-standing.json");
    private static final Field TELEPORT = field(ServerGamePacketListenerImpl.class, "awaitingTeleport");
    private static final List<Map<String, Object>> SAMPLES = new ArrayList<>();
    private static final List<Map<String, Object>> PHASES = new ArrayList<>();
    private static ServerSubLevel grass;
    private static Vector3d plotAnchor;
    private static int initialTeleport;
    private static String active = "free-grass-assembly";

    public static void run(ClientGameTestContext test, TestSingleplayerContext world,
                           List<String> checks) throws Exception {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("scope", "User's stationary grass block: one unconstrained native Rapier grass body "
                + "resting on ordinary stone terrain. Native SDL-scancode player walking steps up via "
                + "an ordinary half slab, stands, and walks off. No constraints, motors, forces, "
                + "repeated wake calls, pose writes or measured player position injection.");
        report.put("samples", SAMPLES); report.put("phases", PHASES);
        try {
            var server = world.getServer();
            server.runCommand("time set noon"); server.runCommand("weather clear");
            server.runCommand("gamemode survival @a");
            server.runOnServer(PackagedGrassStandingRegression::build);
            server.waitFor(s -> grass.getMassTracker().getMass() > 0 && RigidBodyHandle.of(grass) != null, 200);
            for (int i = 0; i < 60; i++) pace(test);
            server.waitFor(s -> RigidBodyHandle.of(grass).getLinearVelocity(new Vector3d()).length() < .04
                    && RigidBodyHandle.of(grass).getAngularVelocity(new Vector3d()).length() < .04, 200);
            test.waitFor(mc -> SubLevelContainer.getContainer(mc.level).getSubLevel(grass.getUniqueId())
                    instanceof ClientSubLevel sub && sub.isFinalized(), 400);
            server.runCommand("tp @a 0.5 121 -2.5 0 15");
            test.runOnClient(mc -> {
                mc.gui.setScreen(null);
                mc.options.autoJump().set(false);
            });
            test.getInput().lookAt(0, 15);
            for (int i = 0; i < 20; i++) pace(test);
            server.runOnServer(s -> initialTeleport = teleport(s));
            report.put("body", grass.getUniqueId().toString());
            report.put("initialTeleportId", initialTeleport);

            active = "step-onto-free-grass";
            List<Map<String, Object>> approach = new ArrayList<>();
            test.getInput().holdKey(options -> options.keyUp);
            try {
                for (int i = 0; i < 50; i++) {
                    var pair = capture(test, world, active, i); approach.add(pair);
                    Map<?, ?> client = (Map<?, ?>) pair.get("client");
                    if (grass.getUniqueId().toString().equals(client.get("tracking"))
                            && number(client, "relativePlot", 2) >= -.30) break;
                }
            } finally { test.getInput().releaseKey(options -> options.keyUp); }

            active = "stand-on-free-grass";
            List<Map<String, Object>> standing = new ArrayList<>();
            for (int i = 0; i < 120; i++) standing.add(capture(test, world, active, i));
            var standMetrics = evaluateStanding(standing);
            PHASES.add(standMetrics);
            var approachMetrics = new LinkedHashMap<String, Object>();
            approachMetrics.put("phase", "step-onto-free-grass"); approachMetrics.put("samples", approach.size());
            approachMetrics.put("enteredGrass", ((Map<?, ?>) approach.getLast().get("client")).get("tracking")
                    .equals(grass.getUniqueId().toString()));
            approachMetrics.put("status", Boolean.TRUE.equals(approachMetrics.get("enteredGrass")) ? "passed" : "failed");
            PHASES.addFirst(approachMetrics);

            active = "step-off-free-grass";
            List<Map<String, Object>> leaving = new ArrayList<>();
            test.getInput().holdKey(options -> options.keyUp);
            try {
                for (int i = 0; i < 50; i++) {
                    var pair = capture(test, world, active, i); leaving.add(pair);
                    if (number((Map<?, ?>) pair.get("client"), "relativePlot", 2) >= 2.0) break;
                }
            } finally { test.getInput().releaseKey(options -> options.keyUp); }
            for (int i = 0; i < 25; i++) leaving.add(capture(test, world, active, leaving.size()));
            var last = leaving.getLast();
            Map<?, ?> finalClient = (Map<?, ?>) last.get("client");
            boolean left = finalClient.get("tracking").equals("none")
                    && Math.abs(number(finalClient, "world", 1) - 121.0) < .1;
            PHASES.add(Map.of("phase", active, "samples", leaving.size(), "leftGrassForRealTerrain", left,
                    "status", left ? "passed" : "failed"));
            report.put("screenshot", test.takeScreenshot("free-grass-standing-complete").toString());
            // Diagnose from the complete trace, including the subsequent step off.
            check(Boolean.TRUE.equals(approachMetrics.get("enteredGrass")), "Native movement did not step onto the actual grass body");
            check(standMetrics.get("status").equals("passed"), "Free grass standing regression: " + standMetrics);
            check(left, "Native player failed to leave the grass body for ordinary terrain");
            for (Map<String, Object> row : SAMPLES) check(((Number) ((Map<?, ?>) row.get("server")).get("teleportId")).intValue() == initialTeleport,
                    "Server movement correction issued during " + row.get("phase"));
            server.runOnServer(s -> s.saveEverything(false, true, true));
            checks.add("one unconstrained real grass body rests on real stone terrain under native gravity; "
                    + "SDL player steps onto it via half slab, stands for120ticks, and steps off; body/client/server "
                    + "trajectories, native teleport counter, support/tracking and camera recorded");
            report.put("status", "passed");
        } catch (Throwable failure) {
            report.put("status", "failed"); report.put("failedPhase", active); report.put("error", failure.toString());
            throw failure instanceof Exception e ? e : new RuntimeException(failure);
        } finally {
            test.getInput().releaseKey(options -> options.keyUp);
            Files.writeString(PROOF, new GsonBuilder().setPrettyPrinting().create().toJson(report));
        }
    }

    private static void build(MinecraftServer server) {
        var level = server.overworld();
        for (int z = -6; z <= 6; z++) for (int x = -5; x <= 5; x++) {
            var ground = new BlockPos(x, 120, z);
            level.getChunk(ground); level.setChunkForced(x >> 4, z >> 4, true);
            level.setBlockAndUpdate(ground, Blocks.STONE.defaultBlockState());
            for (int y = 121; y <= 126; y++) level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
        }
        level.setBlockAndUpdate(new BlockPos(0, 121, -1), Blocks.STONE_SLAB.defaultBlockState()
                .setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        level.setBlockAndUpdate(SOURCE, Blocks.GRASS_BLOCK.defaultBlockState());
        grass = SubLevelAssemblyHelper.assembleBlocks(level, SOURCE, List.of(SOURCE), new BoundingBox3i(SOURCE, SOURCE));
        check(grass != null && !grass.isRemoved(), "Native one-block grass assembly failed");
        plotAnchor = grass.logicalPose().transformPositionInverse(new Vector3d(.5, 121.5, .5));
        check(level.getBlockState(SOURCE).isAir(), "Native assembly left source grass behind");
        check(level.getBlockState(BlockPos.containing(plotAnchor.x, plotAnchor.y, plotAnchor.z)).is(Blocks.GRASS_BLOCK),
                "Actual plot grass block missing");
        check(Sable.HELPER.getContaining(level, plotAnchor.x, plotAnchor.z) == grass, "Grass owner plot identity wrong");
    }

    private static Map<String, Object> capture(ClientGameTestContext test, TestSingleplayerContext world,
                                               String phase, int index) throws Exception {
        pace(test);
        var client = test.computeOnClient(mc -> {
            var sub = SubLevelContainer.getContainer(mc.level).getSubLevel(grass.getUniqueId());
            var row = sample(mc.player, sub);
            var camera = mc.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
            row.put("cameraWorld", List.of(camera.pos.x, camera.pos.y, camera.pos.z));
            row.put("cameraPartialTick", camera.cameraEntityPartialTicks);
            row.put("cameraToEyeDistance", camera.pos.distanceTo(mc.player.getEyePosition()));
            return row;
        });
        AtomicReference<Map<String, Object>> serverRow = new AtomicReference<>();
        world.getServer().runOnServer(server -> {
            var row = sample(server.getPlayerList().getPlayers().getFirst(), grass);
            var handle = RigidBodyHandle.of(grass);
            row.put("bodyVelocity", vector(handle.getLinearVelocity(new Vector3d())));
            row.put("bodyAngularVelocity", vector(handle.getAngularVelocity(new Vector3d())));
            row.put("teleportId", teleport(server));
            serverRow.set(row);
        });
        Map<String, Object> pair = new LinkedHashMap<>();
        pair.put("phase", phase); pair.put("sample", index); pair.put("client", client); pair.put("server", serverRow.get());
        SAMPLES.add(pair);
        if (index % 20 == 0) System.out.println("FREE GRASS " + pair);
        return pair;
    }

    private static Map<String, Object> sample(Entity player, SubLevel sub) {
        check(sub != null && !sub.isRemoved(), "Client/server grass body missing");
        Map<String, Object> row = new LinkedHashMap<>();
        var local = sub.logicalPose().transformPositionInverse(player.position());
        row.put("tick", player.tickCount); row.put("gameTime", player.level().getGameTime());
        row.put("world", List.of(player.getX(), player.getY(), player.getZ()));
        row.put("relativePlot", List.of(local.x - plotAnchor.x, local.y - plotAnchor.y, local.z - plotAnchor.z));
        row.put("bodyPosition", vector(sub.logicalPose().position()));
        var q = sub.logicalPose().orientation(); row.put("bodyOrientation", List.of(q.x(), q.y(), q.z(), q.w()));
        var tracking = Sable.HELPER.getTrackingSubLevel(player);
        row.put("tracking", tracking == null ? "none" : tracking.getUniqueId().toString());
        row.put("onGround", player.onGround()); row.put("horizontalCollision", player.horizontalCollision);
        row.put("verticalCollisionBelow", player.verticalCollisionBelow);
        row.put("deltaMovement", List.of(player.getDeltaMovement().x, player.getDeltaMovement().y, player.getDeltaMovement().z));
        var info = ((EntityMovementExtension) player).sable$getCollisionInfo();
        if (info != null) {
            row.put("subLevelHorizontalCollision", info.subLevelHorizontalCollision);
            if (info.inheritedMotion != null) row.put("inheritedMotion", List.of(info.inheritedMotion.x, info.inheritedMotion.y, info.inheritedMotion.z));
        }
        return row;
    }

    private static Map<String, Object> evaluateStanding(List<Map<String, Object>> rows) {
        int tracked = 0, grounded = 0, corrections = 0, supportedHeight = 0;
        double maxStep = 0, maxBodyStep = 0, maxCameraEye = 0, maxDifference = 0;
        for (int i = 0; i < rows.size(); i++) {
            Map<?, ?> client = (Map<?, ?>) rows.get(i).get("client"), server = (Map<?, ?>) rows.get(i).get("server");
            if (grass.getUniqueId().toString().equals(client.get("tracking"))) tracked++;
            if (Boolean.TRUE.equals(client.get("onGround"))) grounded++;
            if (((Number) server.get("teleportId")).intValue() != initialTeleport) corrections++;
            double height = number(client, "relativePlot", 1);
            if (height > .4 && height < .8) supportedHeight++;
            maxCameraEye = Math.max(maxCameraEye, ((Number) client.get("cameraToEyeDistance")).doubleValue());
            maxDifference = Math.max(maxDifference, distance(client, server, "relativePlot"));
            if (i > 6) {
                Map<?, ?> prior = (Map<?, ?>) rows.get(i - 1).get("client");
                long dt = ((Number) client.get("gameTime")).longValue() - ((Number) prior.get("gameTime")).longValue();
                if (dt > 0) {
                    maxStep = Math.max(maxStep, distance(client, prior, "world") / dt);
                    maxBodyStep = Math.max(maxBodyStep, distance(client, prior, "bodyPosition") / dt);
                }
            }
        }
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("phase", "stand-on-free-grass"); metrics.put("samples", rows.size());
        metrics.put("trackedSamples", tracked); metrics.put("groundedSamples", grounded);
        metrics.put("supportedGrassHeightSamples", supportedHeight); metrics.put("correctionSamples", corrections);
        metrics.put("maximumStandingWorldStepPerTick", maxStep); metrics.put("maximumBodyWorldStepPerTick", maxBodyStep);
        metrics.put("maximumCameraEyeDistance", maxCameraEye); metrics.put("maximumClientServerPlotDifference", maxDifference);
        metrics.put("status", tracked >= 110 && grounded >= 110 && supportedHeight >= 110 && corrections == 0
                && maxStep < .12 && maxDifference < .75 ? "passed" : "failed");
        return metrics;
    }

    private static double distance(Map<?, ?> a, Map<?, ?> b, String property) {
        double squared = 0;
        for (int i = 0; i < 3; i++) squared += Math.pow(number(a, property, i) - number(b, property, i), 2);
        return Math.sqrt(squared);
    }
    private static double number(Map<?, ?> row, String property, int index) { return ((Number) ((List<?>) row.get(property)).get(index)).doubleValue(); }
    private static List<Double> vector(org.joml.Vector3dc value) { return List.of(value.x(), value.y(), value.z()); }
    private static int teleport(MinecraftServer server) {
        try { return TELEPORT.getInt(server.getPlayerList().getPlayers().getFirst().connection); }
        catch (IllegalAccessException failure) { throw new RuntimeException(failure); }
    }
    private static Field field(Class<?> owner, String name) {
        try { var field = owner.getDeclaredField(name); field.setAccessible(true); return field; }
        catch (Exception failure) { throw new RuntimeException(failure); }
    }
    private static void pace(ClientGameTestContext test) throws InterruptedException { test.waitTick(); Thread.sleep(40); }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private PackagedGrassStandingRegression() {}
}
