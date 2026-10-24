import com.google.gson.GsonBuilder;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.IControlContraption.RotationMode;
import com.simibubi.create.content.contraptions.bearing.BearingBlock;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import com.simibubi.create.foundation.collision.CollisionList;
import com.simibubi.create.foundation.collision.ContinuousOBBCollider;
import com.simibubi.create.foundation.collision.Matrix3d;
import com.simibubi.create.foundation.collision.OrientedBB;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/** The reported Create case, distinct from Sable/Rapier physical blocks. */
public final class PackagedCreateWalkingRegression {
    private static final BlockPos BEARING = new BlockPos(0, 120, 0);
    private static final Path PROOF = Path.of(System.getProperty("port.smoke.report"))
            .resolveSibling("create-walking.json");
    private static final Field TELEPORT = field("awaitingTeleport");
    private static final boolean SABLE_PRESENT = FabricLoader.getInstance().isModLoaded("sable");
    private static final Field LAST_X = field("lastGoodX"), LAST_Y = field("lastGoodY"), LAST_Z = field("lastGoodZ");
    private static final List<Map<String, Object>> SAMPLES = new ArrayList<>();
    private static final List<Map<String, Object>> PHASES = new ArrayList<>();
    private static ControlledContraptionEntity owner;
    private static int initialTeleport;
    private static String active = "native-bearing-assembly";

    public static void run(ClientGameTestContext test, TestSingleplayerContext world,
                           List<String> checks) throws Exception {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("scope", "User's stationary moving-Create block: one actual grass block owned by "
                + "a genuine assembled Create mechanical bearing at0RPM, ROTATE_NEVER_PLACE. "
                + "Actual Create ContraptionCollider support, player/server movement and camera are observed. "
                + "No Sable sublevel/physics body is constructed. The Create JAR remains unchanged; "
                + "when Simulated is loaded its server-support compatibility is exercised through the native solver.");
        report.put("samples", SAMPLES); report.put("phases", PHASES);
        report.put("sableLoaded", SABLE_PRESENT);
        try {
            if (Boolean.getBoolean("port.smoke.createSupportProbesOnly")) {
                active = "bounded-server-support-probes-only";
                report.put("scope", "Only the new bounded server support responsibility: actual applied production method and native Create solver; no accepted standing or walk-off replay.");
                report.put("supportProbeCases", test.computeOnClient(mc -> boundedProbeCases()));
                checks.add("new bounded native server-support probe: floor-skin support retained; airborne diagonal moving-off-edge contact rejected");
                report.put("status", "passed");
                return;
            }
            var server = world.getServer();
            server.runCommand("time set noon"); server.runCommand("weather clear");
            server.runCommand("gamemode survival @a");
            server.runOnServer(PackagedCreateWalkingRegression::build);
            test.waitFor(mc -> mc.level.getEntity(owner.getId()) instanceof ControlledContraptionEntity entity
                    && entity.getUUID().equals(owner.getUUID()) && entity.hasOwnedStructure(), 400);
            if (FabricLoader.getInstance().isModLoaded("simulated")) {
                report.put("supportProbeCases", test.computeOnClient(mc -> supportProbeCases()));
                checks.add("actual applied server-support method and genuine Create solver: floor positive; air, far floor, wall, ceiling, wall-top step, departing edge and upward motion negative");
            }
            server.runCommand("tp @a 0.5 122.3 0.5 0 15");
            server.runOnServer(s -> initialTeleport = teleport(s));
            test.runOnClient(mc -> { mc.gui.setScreen(null); mc.options.autoJump().set(false); });
            test.getInput().lookAt(0, 15);
            report.put("nativeContraptionUuid", owner.getUUID().toString());
            report.put("initialTeleportId", initialTeleport);
            report.put("actualOwnedBlocks", owner.getContraption().getBlocks().size());
            active = "landing-on-stationary-create-grass";
            for (int i = 0; i < 20; i++) capture(test, world, active, i);
            active = "standing-on-stationary-create-grass";
            List<Map<String, Object>> standing = new ArrayList<>();
            // Include the native 200-tick support-block refresh interval visible in the user's log.
            for (int i = 0; i < 240; i++) standing.add(capture(test, world, active, i));
            Map<String, Object> metrics = standingMetrics(standing);
            PHASES.add(metrics);
            active = "native-sdl-walk-off-create-grass";
            List<Map<String, Object>> walking = new ArrayList<>();
            test.getInput().holdKey(options -> options.keyUp);
            try { for (int i = 0; i < 35; i++) walking.add(capture(test, world, active, i)); }
            finally { test.getInput().releaseKey(options -> options.keyUp); }
            for (int i = 0; i < 20; i++) walking.add(capture(test, world, active, walking.size()));
            Map<?, ?> last = (Map<?, ?>) walking.getLast().get("client");
            boolean left = number(last, "world", 2) > 2 && Math.abs(number(last, "world", 1) - 120) < .1;
            int ghostContact = 0;
            for (var row : walking.subList(walking.size() - 15, walking.size()))
                for (String side : List.of("client", "server"))
                    if (((Number) ((Map<?, ?>) row.get(side)).get("createContactAge")).intValue() >= 0) ghostContact++;
            PHASES.add(Map.of("phase", active, "samples", walking.size(), "leftForOrdinaryTerrain", left,
                    "ghostContactInLast15SamplesBothSides", ghostContact,
                    "status", left && ghostContact == 0 ? "passed" : "failed"));
            report.put("screenshot", test.takeScreenshot("stationary-create-grass-complete").toString());
            check(metrics.get("status").equals("passed"), "Stationary Create grass standing: " + metrics);
            check(left, "Native player did not walk off Create grass onto actual terrain");
            check(ghostContact == 0, "Create retained ghost support after native player walked off");
            for (Map<String, Object> row : SAMPLES) check(((Number) ((Map<?, ?>) row.get("server")).get("teleportId")).intValue() == initialTeleport,
                    "Native server issued movement correction during " + row.get("phase"));
            server.runOnServer(s -> s.saveEverything(false, true, true));
            report.put("status", "passed");
            checks.add("actual Create bearing owns a stationary grass block at0RPM; native client/server "
                    + "support and camera sampled during landing,240 standing ticks across support refresh, "
                    + "and real SDL walking off; no Sable physical fixture or Create JAR changes");
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
        for (int z = -8; z <= 8; z++) for (int x = -6; x <= 6; x++) {
            var pos = new BlockPos(x, 119, z); level.getChunk(pos);
            level.setChunkForced(x >> 4, z >> 4, true);
            level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            for (int y = 120; y <= 126; y++) level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
        }
        level.setBlockAndUpdate(BEARING, AllBlocks.MECHANICAL_BEARING.defaultBlockState()
                .setValue(BearingBlock.FACING, Direction.UP));
        level.setBlockAndUpdate(BEARING.above(), Blocks.GRASS_BLOCK.defaultBlockState());
        var bearing = (MechanicalBearingBlockEntity) level.getBlockEntity(BEARING);
        bearing.setMovementMode(RotationMode.ROTATE_NEVER_PLACE);
        bearing.assemble();
        owner = bearing.getMovedContraption();
        check(bearing.isRunning() && owner != null && owner.hasOwnedStructure(),
                "Actual Create bearing assembly failed: " + bearing.getLastAssemblyException());
        check(bearing.getSpeed() == 0 && owner.getContraption().getBlocks().size() == 1,
                "Fixture must be stationary and own exactly one real block");
        check(owner.getContraption().getBlocks().get(BlockPos.ZERO).state().is(Blocks.GRASS_BLOCK),
                "Native contraption grass payload missing");
        check(level.getBlockState(BEARING.above()).isAir(), "Grass was not removed into native contraption storage");
    }

    private static Map<String, Object> capture(ClientGameTestContext test, TestSingleplayerContext world,
                                               String phase, int index) throws Exception {
        test.waitTick(); Thread.sleep(40);
        Map<String, Object> client = test.computeOnClient(mc -> {
            var body = (ControlledContraptionEntity) mc.level.getEntity(owner.getId());
            var row = sample(mc.player, body);
            var camera = mc.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
            row.put("cameraWorld", List.of(camera.pos.x, camera.pos.y, camera.pos.z));
            row.put("cameraPartialTick", camera.cameraEntityPartialTicks);
            row.put("cameraEyeDistance", camera.pos.distanceTo(mc.player.getEyePosition()));
            row.put("isNativeCreateLocalPlayer", com.simibubi.create.content.contraptions.ContraptionCollider.isLocalPlayer.test(mc.player));
            return row;
        });
        AtomicReference<Map<String, Object>> observed = new AtomicReference<>();
        world.getServer().runOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            var row = sample(player, owner);
            row.put("teleportId", teleport(server));
            row.put("nativeLastGood", List.of(read(LAST_X, player.connection), read(LAST_Y, player.connection), read(LAST_Z, player.connection)));
            var bearing = (MechanicalBearingBlockEntity) server.overworld().getBlockEntity(BEARING);
            row.put("bearingRunning", bearing.isRunning()); row.put("bearingRpm", bearing.getSpeed());
            observed.set(row);
        });
        Map<String, Object> pair = new LinkedHashMap<>();
        pair.put("phase", phase); pair.put("sample", index); pair.put("client", client); pair.put("server", observed.get());
        SAMPLES.add(pair);
        if (index % 20 == 0) System.out.println("CREATE GRASS " + pair);
        return pair;
    }

    private static Map<String, Object> sample(Entity player, ControlledContraptionEntity body) {
        check(body != null && body.isAlive() && body.hasOwnedStructure(), "Native Create contraption disappeared");
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("gameTime", player.level().getGameTime()); row.put("tick", player.tickCount);
        row.put("world", List.of(player.getX(), player.getY(), player.getZ()));
        var local = body.toLocalVector(player.position(), 1);
        row.put("contraptionLocal", List.of(local.x, local.y, local.z));
        row.put("contraptionPosition", List.of(body.getX(), body.getY(), body.getZ()));
        row.put("contraptionAngle", body.getAngle(1)); row.put("contraptionStalled", body.isStalled());
        var contact = body.collidingEntities.get(player);
        row.put("createContactAge", contact == null ? -1 : contact.intValue());
        row.put("onGround", player.onGround()); row.put("horizontalCollision", player.horizontalCollision);
        row.put("verticalCollisionBelow", player.verticalCollisionBelow);
        row.put("deltaMovement", List.of(player.getDeltaMovement().x, player.getDeltaMovement().y, player.getDeltaMovement().z));
        sampleOptionalSable(player, row);
        return row;
    }

    private static void sampleOptionalSable(Entity player, Map<String, Object> row) {
        if (!SABLE_PRESENT) {
            row.put("sableTracking", "mod-not-loaded");
            row.put("sablePlotCount", 0);
            return;
        }
        try {
            Object tracked = player.getClass().getMethod("sable$getTrackingSubLevel").invoke(player);
            row.put("sableTracking", tracked == null ? "none"
                    : tracked.getClass().getMethod("getUniqueId").invoke(tracked).toString());
            Class<?> containers = Class.forName("dev.ryanhcode.sable.api.sublevel.SubLevelContainer");
            Object container = containers.getMethod("getContainer", net.minecraft.world.level.Level.class)
                    .invoke(null, player.level());
            int plots = 0;
            for (Object ignored : (Iterable<?>) containers.getMethod("getAllSubLevels").invoke(container)) plots++;
            row.put("sablePlotCount", plots);
            Object info = player.getClass().getMethod("sable$getCollisionInfo").invoke(player);
            if (info != null) {
                row.put("sableVerticalCollision", info.getClass().getField("verticalCollision").getBoolean(info));
                row.put("sableVerticalCollisionBelow", info.getClass().getField("verticalCollisionBelow").getBoolean(info));
                row.put("sableHorizontalCollision", info.getClass().getField("horizontalCollision").getBoolean(info));
            }
        } catch (ReflectiveOperationException failure) {
            throw new RuntimeException("Loaded Sable diagnostic API could not be observed", failure);
        }
    }

    private static Map<String, Object> standingMetrics(List<Map<String, Object>> rows) {
        int supported = 0, clientContact = 0, serverContact = 0, ground = 0, corrections = 0;
        double maxStep = 0, maxCamera = 0, maxDifference = 0;
        for (int i = 0; i < rows.size(); i++) {
            Map<?, ?> c = (Map<?, ?>) rows.get(i).get("client"), s = (Map<?, ?>) rows.get(i).get("server");
            if (Math.abs(number(c, "world", 1) - 122) < .08) supported++;
            if (((Number) c.get("createContactAge")).intValue() >= 0) clientContact++;
            if (((Number) s.get("createContactAge")).intValue() >= 0) serverContact++;
            if (Boolean.TRUE.equals(c.get("onGround"))) ground++;
            if (((Number) s.get("teleportId")).intValue() != initialTeleport) corrections++;
            maxCamera = Math.max(maxCamera, ((Number) c.get("cameraEyeDistance")).doubleValue());
            maxDifference = Math.max(maxDifference, distance(c, s));
            if (i > 0) {
                var prior = (Map<?, ?>) rows.get(i - 1).get("client");
                long dt = ((Number) c.get("gameTime")).longValue() - ((Number) prior.get("gameTime")).longValue();
                if (dt > 0) maxStep = Math.max(maxStep, distance(c, prior) / dt);
            }
        }
        var metrics = new LinkedHashMap<String, Object>();
        metrics.put("phase", "standing-on-stationary-create-grass"); metrics.put("samples", rows.size());
        metrics.put("supportedHeightSamples", supported); metrics.put("clientContactSamples", clientContact);
        metrics.put("serverContactSamples", serverContact); metrics.put("groundedSamples", ground);
        metrics.put("correctionSamples", corrections); metrics.put("maximumWorldStepPerTick", maxStep);
        metrics.put("maximumClientServerDifference", maxDifference); metrics.put("maximumCameraEyeDistance", maxCamera);
        metrics.put("status", supported >= 228 && clientContact >= 228 && serverContact == rows.size() && ground >= 216
                && corrections == 0 && maxStep < .12 && maxDifference < .75 ? "passed" : "failed");
        return metrics;
    }

    private static Map<String, Object> supportProbeCases() throws Exception {
        var method = java.util.Arrays.stream(com.simibubi.create.content.contraptions.ContraptionCollider.class.getDeclaredMethods())
                .filter(value -> value.getName().contains("retryStandingSupport") && value.getParameterCount() == 6)
                .findFirst().orElseThrow(() -> new AssertionError("Actual applied Create support mixin method missing"));
        method.setAccessible(true);
        Map<String, Object> cases = new LinkedHashMap<>();
        probe(cases, method, "floor-skin-contact", true, new double[]{0, 0, 0, 1, 1, 1}, .5, 1.0001001, .5, Vec3.ZERO);
        probe(cases, method, "empty-air", false, null, .5, 1.0001001, .5, Vec3.ZERO);
        probe(cases, method, "floor-too-far", false, new double[]{0, 0, 0, 1, 1, 1}, .5, 1.01, .5, Vec3.ZERO);
        probe(cases, method, "wall-side", false, new double[]{0, 0, 0, 1, 3, 1}, 1.3001, .5, .5, Vec3.ZERO);
        probe(cases, method, "ceiling", false, new double[]{0, 2, 0, 1, 3, 1}, .5, .2001, .5, Vec3.ZERO);
        probe(cases, method, "wall-top-within-step-height", false, new double[]{0, 0, 0, 1, 1.6, 1}, 1.25, 1.0001, .5, Vec3.ZERO);
        probe(cases, method, "departed-floor-edge", false, new double[]{0, 0, 0, 1, 1, 1}, .5, 1.0001001, 1.3001, new Vec3(0, 0, .1));
        probe(cases, method, "upward-jump", false, new double[]{0, 0, 0, 1, 1, 1}, .5, 1.0001001, .5, new Vec3(0, .01, 0));
        return cases;
    }

    private static Map<String, Object> boundedProbeCases() throws Exception {
        var method = java.util.Arrays.stream(com.simibubi.create.content.contraptions.ContraptionCollider.class.getDeclaredMethods())
                .filter(value -> value.getName().contains("retryStandingSupport") && value.getParameterCount() == 6)
                .findFirst().orElseThrow(() -> new AssertionError("Actual bounded support mixin method missing"));
        method.setAccessible(true);
        Map<String, Object> cases = new LinkedHashMap<>();
        probe(cases, method, "fixed-floor-skin-contact", true, new double[]{0, 0, 0, 1, 1, 1}, .5, 1.0001001, .5, Vec3.ZERO);
        double feet = 1.5000001;
        double gap = Math.abs((feet + .9) - .5) - (.5 + .9000001);
        probe(cases, method, "airborne-diagonal-leaving-floor", false, new double[]{0, 0, 0, 1, 1, 1},
                .9, feet, .5, new Vec3(1, -gap, 0));
        return cases;
    }

    private static void probe(Map<String, Object> cases, java.lang.reflect.Method method, String name,
                              boolean expected, double[] shape, double x, double feet, double z, Vec3 motion) throws Exception {
        CollisionList shapes = new CollisionList();
        if (shape != null) new CollisionList.Populate(shapes).consume(shape[0], shape[1], shape[2], shape[3], shape[4], shape[5]);
        Matrix3d rotation = new Matrix3d().asIdentity();
        OrientedBB box = new OrientedBB(new Vec3(x, feet + .9, z), new Vec3(.3000001, .9000001, .3000001), rotation);
        var original = ContinuousOBBCollider.collideMany(shapes, new CollisionList(), box, motion, .6f, true);
        var result = (ContinuousOBBCollider.CollisionResponse) method.invoke(null, shapes, new CollisionList(), box, motion, rotation, original);
        var normal = rotation.transformTransposed(result.normal).normalize();
        boolean supported = result.surfaceCollision && normal.y > .4;
        cases.put(name, Map.of("expectedSupport", expected, "actualSupport", supported,
                "originalSurfaceCollision", original.surfaceCollision, "worldNormal", List.of(normal.x, normal.y, normal.z),
                "status", supported == expected ? "passed" : "failed"));
        check(supported == expected, "Actual native support probe " + name + ": " + cases.get(name));
    }
    private static double number(Map<?, ?> row, String key, int i) { return ((Number) ((List<?>) row.get(key)).get(i)).doubleValue(); }
    private static double distance(Map<?, ?> a, Map<?, ?> b) {
        double d = 0; for (int i = 0; i < 3; i++) d += Math.pow(number(a, "world", i) - number(b, "world", i), 2);
        return Math.sqrt(d);
    }
    private static int teleport(MinecraftServer server) {
        try { return TELEPORT.getInt(server.getPlayerList().getPlayers().getFirst().connection); }
        catch (IllegalAccessException failure) { throw new RuntimeException(failure); }
    }
    private static double read(Field field, Object value) {
        try { return field.getDouble(value); } catch (IllegalAccessException failure) { throw new RuntimeException(failure); }
    }
    private static Field field(String name) {
        try { var field = ServerGamePacketListenerImpl.class.getDeclaredField(name); field.setAccessible(true); return field; }
        catch (Exception failure) { throw new RuntimeException(failure); }
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private PackagedCreateWalkingRegression() {}
}
