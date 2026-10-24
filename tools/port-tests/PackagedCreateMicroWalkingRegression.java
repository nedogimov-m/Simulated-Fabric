import com.google.gson.GsonBuilder;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.IControlContraption.RotationMode;
import com.simibubi.create.content.contraptions.bearing.BearingBlock;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

/** Real forward walking across native Create grass block seams, with micro-stutter observations. */
public final class PackagedCreateMicroWalkingRegression {
    private static final BlockPos BEARING = new BlockPos(0, 120, 0);
    private static final Path PROOF = Path.of(System.getProperty("port.smoke.report"))
            .resolveSibling("create-micro-walking.json");
    private static final Field TELEPORT = field("awaitingTeleport");
    private static final Field AWAITING_POSITION = field("awaitingPositionFromClient");
    private static final Field LAST_X = field("lastGoodX"), LAST_Y = field("lastGoodY"), LAST_Z = field("lastGoodZ");
    private static final ConcurrentLinkedQueue<Map<String, Object>> CLIENT = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<Map<String, Object>> SERVER = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<Map<String, Object>> EXTRACTED = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<Map<String, Object>> RENDERED = new ConcurrentLinkedQueue<>();
    private static final AtomicReference<Throwable> OBSERVER_FAILURE = new AtomicReference<>();
    private static volatile boolean recording;
    private static ControlledContraptionEntity owner;
    private static int initialTeleport;

    public static void run(ClientGameTestContext test, TestSingleplayerContext world,
                           List<String> checks) throws Exception {
        var report = new LinkedHashMap<String, Object>();
        report.put("scope", "Native SDL held-forward walking across a genuine assembled stationary Create "
                + "3x41 grass platform (123 blocks), upward mechanical bearing, 0RPM, ROTATE_NEVER_PLACE. "
                + "No Sable platform is assembled. Placement teleport is acknowledged before measurement; "
                + "there is no direct player movement or position injection during measurement.");
        report.put("measurement", "Every END_CLIENT_TICK and END_SERVER_TICK plus every camera extraction "
                + "and END_MAIN rendered frame. Forward is world +Z. Backward threshold is 0.0001 blocks. "
                + "Client and server observations are asynchronous; raw positions and monotonic observation "
                + "times are retained. Equal gameTime comparison is diagnostic, not an exact synchronization assertion.");
        String phase = "assemble-native-create-platform";
        try {
            var server = world.getServer();
            server.runCommand("time set noon");
            server.runCommand("weather clear");
            server.runCommand("gamemode survival @a");
            server.runOnServer(PackagedCreateMicroWalkingRegression::build);
            test.waitFor(mc -> mc.level.getEntity(owner.getId()) instanceof ControlledContraptionEntity body
                    && body.getUUID().equals(owner.getUUID()) && body.hasOwnedStructure(), 400);
            registerObservers();
            phase = "placement-and-teleport-acknowledgement";
            server.runCommand("tp @a 0.5 122.3 -15.5 0 15");
            test.runOnClient(mc -> { mc.gui.setScreen(null); mc.options.autoJump().set(false); });
            test.getInput().lookAt(0, 15);
            for (int i = 0; i < 20; i++) pace(test);
            server.waitFor(s -> {
                var player = s.getPlayerList().getPlayers().getFirst();
                return read(AWAITING_POSITION, player.connection) == null
                        && owner.collidingEntities.containsKey(player) && Math.abs(player.getY() - 122) < .002;
            }, 200);
            test.waitFor(mc -> {
                var body = (ControlledContraptionEntity) mc.level.getEntity(owner.getId());
                return body != null && body.collidingEntities.containsKey(mc.player)
                        && mc.player.onGround() && Math.abs(mc.player.getY() - 122) < .002;
            }, 200);
            server.runOnServer(s -> {
                var player = s.getPlayerList().getPlayers().getFirst();
                check(read(AWAITING_POSITION, player.connection) == null, "Placement teleport was not acknowledged");
                initialTeleport = ((Number) read(TELEPORT, player.connection)).intValue();
                report.put("serverBeforeWalking", serverSample(s));
            });
            test.runOnClient(mc -> {
                check(!mc.player.getAbilities().flying, "Fixture player must walk, not fly");
                report.put("clientBeforeWalking", clientSample(mc));
                report.put("viewBobbing", mc.options.bobView().get());
            });
            report.put("nativeContraptionUuid", owner.getUUID().toString());
            report.put("actualOwnedGrassBlocks", owner.getContraption().getBlocks().size());
            report.put("initialTeleportId", initialTeleport);
            phase = "native-sdl-forward-across-create-grass-seams";
            test.getInput().holdKey(options -> options.keyUp);
            recording = true;
            for (int i = 0; i < 100; i++) pace(test);
            recording = false;
            test.getInput().releaseKey(options -> options.keyUp);
            test.runOnClient(mc -> report.put("clientAfterWalking", clientSample(mc)));
            server.runOnServer(s -> report.put("serverAfterWalking", serverSample(s)));
            if (OBSERVER_FAILURE.get() != null) throw new RuntimeException("Native observer failed", OBSERVER_FAILURE.get());
            var client = new ArrayList<>(CLIENT);
            var serverRows = new ArrayList<>(SERVER);
            var extracted = new ArrayList<>(EXTRACTED);
            var rendered = new ArrayList<>(RENDERED);
            var cm = motionMetrics(client, "world");
            var sm = motionMetrics(serverRows, "world");
            var em = motionMetrics(extracted, "cameraWorld");
            var rm = motionMetrics(rendered, "cameraWorld");
            report.put("clientMetrics", cm);
            report.put("serverMetrics", sm);
            report.put("extractedCameraMetrics", em);
            report.put("renderedCameraMetrics", rm);
            report.put("cameraInterpolationResidualMetrics", residualMetrics(extracted));
            report.put("asynchronousClientServerMetrics", discrepancyMetrics(client, serverRows));
            int corrections = 0;
            for (var row : serverRows)
                if (((Number) row.get("teleportId")).intValue() != initialTeleport) corrections++;
            report.put("serverCorrectionSamples", corrections);
            report.put("screenshot", test.takeScreenshot("create-grass-micro-walking-complete").toString());
            System.out.println("CREATE MICRO WALK client=" + cm + " server=" + sm + " camera=" + rm);
            check(client.size() >= 95 && serverRows.size() >= 65, "Insufficient actual tick observations");
            check(((Number) cm.get("forwardTravel")).doubleValue() > 15, "Native SDL input did not walk across grass seams");
            check(((Number) cm.get("backwardStepsOverThreshold")).intValue() == 0, "Client micro rubberbanding observed: " + cm);
            check(((Number) sm.get("backwardStepsOverThreshold")).intValue() == 0, "Server micro rubberbanding observed: " + sm);
            check(((Number) rm.get("backwardStepsOverThreshold")).intValue() == 0, "Rendered camera moved backwards: " + rm);
            check(((Number) cm.get("maximumAbsoluteYStep")).doubleValue() < .005, "Client vertical micro-jump observed: " + cm);
            check(((Number) sm.get("maximumAbsoluteYStep")).doubleValue() < .005, "Server vertical micro-jump observed: " + sm);
            check(((Number) cm.get("contactSamples")).intValue() >= client.size() * .95,
                    "Client lost native Create support during walking: " + cm);
            check(((Number) sm.get("contactSamples")).intValue() >= serverRows.size() * .95,
                    "Server lost native Create support during walking: " + sm);
            check(corrections == 0, "Server issued a movement correction while walking");
            check(rendered.size() >= 50 && extracted.size() >= 50, "Insufficient real rendered camera frames");
            report.put("status", "passed");
            checks.add("real SDL forward walking across 123 stationary native Create grass blocks: "
                    + "every client/server tick and rendered camera frame retained; 0.0001-block backwards "
                    + "steps, vertical micro-jumps, native contact and movement correction IDs checked");
            server.runOnServer(s -> s.saveEverything(false, true, true));
        } catch (Throwable failure) {
            report.put("status", "failed");
            report.put("failedPhase", phase);
            report.put("error", failure.toString());
            throw failure instanceof Exception e ? e : new RuntimeException(failure);
        } finally {
            recording = false;
            test.getInput().releaseKey(options -> options.keyUp);
            report.put("clientTicks", new ArrayList<>(CLIENT));
            report.put("serverTicks", new ArrayList<>(SERVER));
            report.put("cameraExtractions", new ArrayList<>(EXTRACTED));
            report.put("renderedCameraFrames", new ArrayList<>(RENDERED));
            Files.writeString(PROOF, new GsonBuilder().setPrettyPrinting().create().toJson(report));
        }
    }

    private static void build(MinecraftServer server) {
        var level = server.overworld();
        for (int z = -23; z <= 23; z++) for (int x = -4; x <= 4; x++) {
            var pos = new BlockPos(x, 119, z);
            level.getChunk(pos);
            level.setChunkForced(x >> 4, z >> 4, true);
            level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            for (int y = 120; y <= 126; y++) level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
        }
        level.setBlockAndUpdate(BEARING, AllBlocks.MECHANICAL_BEARING.defaultBlockState()
                .setValue(BearingBlock.FACING, Direction.UP));
        for (int z = -20; z <= 20; z++) for (int x = -1; x <= 1; x++)
            level.setBlockAndUpdate(new BlockPos(x, 121, z), Blocks.GRASS_BLOCK.defaultBlockState());
        check(level.addFreshEntity(new SuperGlueEntity(level, new AABB(-1, 121, -20, 2, 122, 21))),
                "Native platform assembly glue was rejected");
        var bearing = (MechanicalBearingBlockEntity) level.getBlockEntity(BEARING);
        bearing.setMovementMode(RotationMode.ROTATE_NEVER_PLACE);
        bearing.assemble();
        owner = bearing.getMovedContraption();
        check(bearing.isRunning() && owner != null && owner.hasOwnedStructure(),
                "Native Create assembly failed: " + bearing.getLastAssemblyException());
        check(bearing.getSpeed() == 0 && owner.getContraption().getBlocks().size() == 123,
                "Fixture must own 123 grass blocks at 0RPM");
        for (var block : owner.getContraption().getBlocks().values())
            check(block.state().is(Blocks.GRASS_BLOCK), "Fixture owns a block other than grass");
        for (int z = -20; z <= 20; z++) for (int x = -1; x <= 1; x++)
            check(level.getBlockState(new BlockPos(x, 121, z)).isAir(), "Grass source was not assembled");
    }

    private static void registerObservers() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> observe(() -> CLIENT.add(clientSample(mc))));
        ServerTickEvents.END_SERVER_TICK.register(server -> observe(() -> SERVER.add(serverSample(server))));
        LevelExtractionEvents.END_EXTRACTION.register(context -> observe(() -> {
            var state = context.levelState();
            var row = cameraSample(state);
            var player = Minecraft.getInstance().player;
            var eye = player.getEyePosition(state.cameraRenderState.cameraEntityPartialTicks);
            row.put("interpolatedPlayerEye", vector(eye));
            row.put("cameraMinusInterpolatedEye", vector(state.cameraRenderState.pos.subtract(eye)));
            row.put("playerWorld", vector(player.position()));
            row.put("playerPreviousWorld", List.of(player.xo, player.yo, player.zo));
            row.put("playerTick", player.tickCount);
            EXTRACTED.add(row);
        }));
        LevelRenderEvents.END_MAIN.register(context -> observe(() -> RENDERED.add(cameraSample(context.levelState()))));
    }

    private static void observe(Runnable observation) {
        if (!recording) return;
        try { observation.run(); }
        catch (Throwable failure) { OBSERVER_FAILURE.compareAndSet(null, failure); }
    }

    private static Map<String, Object> clientSample(Minecraft mc) {
        var row = sample(mc.player, (ControlledContraptionEntity) mc.level.getEntity(owner.getId()));
        row.put("forwardKeyDown", mc.options.keyUp.isDown());
        row.put("flying", mc.player.getAbilities().flying);
        row.put("sprinting", mc.player.isSprinting());
        row.put("isNativeCreateLocalPlayer", com.simibubi.create.content.contraptions.ContraptionCollider.isLocalPlayer.test(mc.player));
        return row;
    }

    private static Map<String, Object> serverSample(MinecraftServer server) {
        var player = server.getPlayerList().getPlayers().getFirst();
        var row = sample(player, owner);
        row.put("teleportId", read(TELEPORT, player.connection));
        row.put("awaitingTeleportAcknowledgement", read(AWAITING_POSITION, player.connection) != null);
        row.put("nativeLastGood", List.of(read(LAST_X, player.connection), read(LAST_Y, player.connection), read(LAST_Z, player.connection)));
        var bearing = (MechanicalBearingBlockEntity) server.overworld().getBlockEntity(BEARING);
        row.put("bearingRpm", bearing.getSpeed());
        row.put("bearingRunning", bearing.isRunning());
        return row;
    }

    private static Map<String, Object> sample(Entity player, ControlledContraptionEntity body) {
        check(body != null && body.isAlive() && body.hasOwnedStructure(), "Native Create structure disappeared");
        var row = new LinkedHashMap<String, Object>();
        row.put("observationNanos", System.nanoTime());
        row.put("gameTime", player.level().getGameTime());
        row.put("playerTick", player.tickCount);
        row.put("world", vector(player.position()));
        row.put("previousWorld", List.of(player.xo, player.yo, player.zo));
        row.put("deltaMovement", vector(player.getDeltaMovement()));
        row.put("contraptionLocal", vector(body.toLocalVector(player.position(), 1)));
        row.put("contraptionPosition", vector(body.position()));
        row.put("contraptionAngle", body.getAngle(1));
        var contact = body.collidingEntities.get(player);
        row.put("createContactAge", contact == null ? -1 : contact.intValue());
        row.put("onGround", player.onGround());
        row.put("verticalCollisionBelow", player.verticalCollisionBelow);
        row.put("horizontalCollision", player.horizontalCollision);
        if (FabricLoader.getInstance().isModLoaded("sable")) try {
            Object tracking = player.getClass().getMethod("sable$getTrackingSubLevel").invoke(player);
            row.put("sableTracking", tracking == null ? "none" : tracking.toString());
        } catch (ReflectiveOperationException failure) { throw new RuntimeException(failure); }
        return row;
    }

    private static Map<String, Object> cameraSample(net.minecraft.client.renderer.state.level.LevelRenderState state) {
        var row = new LinkedHashMap<String, Object>();
        row.put("observationNanos", System.nanoTime());
        row.put("gameTime", state.gameTime);
        row.put("worldPartialTick", state.worldPartialTicks);
        row.put("cameraPartialTick", state.cameraRenderState.cameraEntityPartialTicks);
        row.put("cameraWorld", vector(state.cameraRenderState.pos));
        return row;
    }

    private static Map<String, Object> motionMetrics(List<Map<String, Object>> rows, String position) {
        var result = new LinkedHashMap<String, Object>();
        int backwards = 0, contacts = 0, grounded = 0, steadySlow = 0;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        double maxBackward = 0, maxYStep = 0, maxForward = 0;
        for (int i = 0; i < rows.size(); i++) {
            var row = rows.get(i);
            minY = Math.min(minY, coordinate(row, position, 1));
            maxY = Math.max(maxY, coordinate(row, position, 1));
            if (row.get("createContactAge") instanceof Number age && age.intValue() >= 0) contacts++;
            if (Boolean.TRUE.equals(row.get("onGround"))) grounded++;
            if (i == 0) continue;
            var previous = rows.get(i - 1);
            double dz = coordinate(row, position, 2) - coordinate(previous, position, 2);
            double dy = coordinate(row, position, 1) - coordinate(previous, position, 1);
            if (dz < -.0001) backwards++;
            maxBackward = Math.max(maxBackward, -dz);
            maxForward = Math.max(maxForward, dz);
            maxYStep = Math.max(maxYStep, Math.abs(dy));
            long dt = ((Number) row.get("gameTime")).longValue() - ((Number) previous.get("gameTime")).longValue();
            if (i > 5 && dt > 0 && dz / dt < .1) steadySlow++;
        }
        result.put("samples", rows.size());
        if (rows.isEmpty()) return result;
        result.put("forwardTravel", coordinate(rows.getLast(), position, 2) - coordinate(rows.getFirst(), position, 2));
        result.put("firstGameTime", rows.getFirst().get("gameTime"));
        result.put("lastGameTime", rows.getLast().get("gameTime"));
        result.put("backwardThresholdBlocks", .0001);
        result.put("backwardStepsOverThreshold", backwards);
        result.put("maximumBackwardStep", maxBackward);
        result.put("maximumForwardStep", maxForward);
        result.put("maximumAbsoluteYStep", maxYStep);
        result.put("minimumY", minY);
        result.put("maximumY", maxY);
        result.put("yRange", maxY - minY);
        result.put("steadySamplesUnderPointOneBlocksPerGameTick", steadySlow);
        result.put("contactSamples", contacts);
        result.put("groundedSamples", grounded);
        return result;
    }

    private static Map<String, Object> residualMetrics(List<Map<String, Object>> rows) {
        double maximum = 0, minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxYStep = 0;
        for (int i = 0; i < rows.size(); i++) {
            var row = rows.get(i);
            double y = coordinate(row, "cameraMinusInterpolatedEye", 1);
            minY = Math.min(minY, y); maxY = Math.max(maxY, y);
            maximum = Math.max(maximum, Math.sqrt(Math.pow(coordinate(row, "cameraMinusInterpolatedEye", 0), 2)
                    + y * y + Math.pow(coordinate(row, "cameraMinusInterpolatedEye", 2), 2)));
            if (i > 0) maxYStep = Math.max(maxYStep, Math.abs(y - coordinate(rows.get(i - 1), "cameraMinusInterpolatedEye", 1)));
        }
        return rows.isEmpty() ? Map.of("samples", 0) : Map.of("samples", rows.size(),
                "maximumDistanceFromInterpolatedEye", maximum, "minimumYResidual", minY,
                "maximumYResidual", maxY, "yResidualRange", maxY - minY, "maximumYResidualStep", maxYStep);
    }

    private static Map<String, Object> discrepancyMetrics(List<Map<String, Object>> client, List<Map<String, Object>> server) {
        double maximum = 0;
        long maximumObservationSkew = 0;
        int paired = 0;
        for (var c : client) for (var s : server) if (c.get("gameTime").equals(s.get("gameTime"))) {
            double distance = 0;
            for (int axis = 0; axis < 3; axis++) distance += Math.pow(coordinate(c, "world", axis) - coordinate(s, "world", axis), 2);
            maximum = Math.max(maximum, Math.sqrt(distance));
            maximumObservationSkew = Math.max(maximumObservationSkew,
                    Math.abs(((Number) c.get("observationNanos")).longValue() - ((Number) s.get("observationNanos")).longValue()));
            paired++;
            break;
        }
        return Map.of("equalGameTimePairs", paired, "maximumEqualGameTimePositionDifference", maximum,
                "maximumEqualGameTimeObservationSkewMillis", maximumObservationSkew / 1_000_000.0,
                "interpretation", "Separate native ticks; gameTime equality does not guarantee identical packet processing phase.");
    }

    private static double coordinate(Map<String, Object> row, String key, int axis) {
        return ((Number) ((List<?>) row.get(key)).get(axis)).doubleValue();
    }
    private static List<Double> vector(Vec3 value) { return List.of(value.x, value.y, value.z); }
    private static Object read(Field field, Object object) {
        try { return field.get(object); } catch (IllegalAccessException failure) { throw new RuntimeException(failure); }
    }
    private static Field field(String name) {
        try { var field = ServerGamePacketListenerImpl.class.getDeclaredField(name); field.setAccessible(true); return field; }
        catch (ReflectiveOperationException failure) { throw new RuntimeException(failure); }
    }
    private static void pace(ClientGameTestContext test) throws InterruptedException { test.waitTick(); Thread.sleep(40); }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private PackagedCreateMicroWalkingRegression() {}
}
