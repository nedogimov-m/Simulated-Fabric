import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintHandle;
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
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/** Actual native player input and client/server movement on one Rapier platform. */
public final class PackagedPhysicalWalkingRegression {
    private static final BlockPos ANCHOR = new BlockPos(0, 120, 0);
    private static final Path PROOF = Path.of(System.getProperty("port.smoke.report"))
            .resolveSibling("physical-walking.json");
    private static final Field TELEPORT_ID = field(ServerGamePacketListenerImpl.class, "awaitingTeleport");
    private static final boolean ROTATED = Boolean.getBoolean("port.smoke.physicalRotation");
    private static final Quaterniond GUIDE_ORIENTATION = ROTATED
            ? new Quaterniond().rotateY(Math.toRadians(17)).rotateX(Math.toRadians(6)) : new Quaterniond();
    private static final List<Map<String, Object>> SAMPLES = new ArrayList<>();
    private static final List<Map<String, Object>> PHASES = new ArrayList<>();
    private static ServerSubLevel platform;
    private static GenericConstraintHandle joint;
    private static Vector3d localAnchor;
    private static int baselineTeleportId;
    private static long movementStart;
    private static String active = "construct";

    public static void run(ClientGameTestContext test, TestSingleplayerContext world,
                           List<String> checks) throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scope", "Native SDL-scancode KeyEvent walking on actual assembled Sable blocks; "
                + "Rapier joint motor supplies platform translation. No direct player movement, "
                + "position injection, disabled collision, or movement-check changes during measured phases.");
        result.put("samples", SAMPLES);
        result.put("phases", PHASES);
        try {
            var server = world.getServer();
            server.runCommand("time set noon");
            server.runCommand("weather clear");
            server.runCommand("gamemode survival @a");
            server.runOnServer(PackagedPhysicalWalkingRegression::build);
            server.waitFor(s -> platform.getMassTracker().getMass() > 0
                    && RigidBodyHandle.of(platform) != null, 200);
            server.runOnServer(PackagedPhysicalWalkingRegression::attach);
            if (ROTATED) server.waitFor(s -> Math.abs(platform.logicalPose().orientation().dot(GUIDE_ORIENTATION)) > .999999
                    && RigidBodyHandle.of(platform).getAngularVelocity(new Vector3d()).length() < .02, 200);
            test.waitFor(client -> SubLevelContainer.getContainer(client.level)
                    .getSubLevel(platform.getUniqueId()) instanceof ClientSubLevel sub && sub.isFinalized(), 400);
            if (ROTATED) server.runOnServer(s -> {
                var spawn = platform.logicalPose().transformPosition(new Vector3d(localAnchor).add(0, 1.5, -15));
                s.getCommands().performPrefixedCommand(s.createCommandSourceStack(), String.format(java.util.Locale.ROOT,
                        "tp @a %.8f %.8f %.8f -17 15", spawn.x, spawn.y, spawn.z));
            });
            else server.runCommand("tp @a 0.5 122 -14.5 0 15");
            test.runOnClient(client -> {
                client.gui.setScreen(null);
                client.options.autoJump().set(false);
            });
            test.getInput().lookAt(ROTATED ? -17 : 0, 15);
            for (int i = 0; i < 40; i++) pace(test);
            server.runOnServer(s -> baselineTeleportId = teleportId(s));
            result.put("platform", platform.getUniqueId().toString());
            result.put("initialTeleportId", baselineTeleportId);
            result.put("requestedYawDegrees", ROTATED ? 17 : 0);
            result.put("requestedPitchDegrees", ROTATED ? 6 : 0);
            if (ROTATED) {
                retainAlignedAcceptance(result);
                phase(test, world, "rotated-static-full-block-seams", 40, true, false, false);
                server.runOnServer(s -> movementStart = s.overworld().getGameTime());
                phase(test, world, "rotated-moving-full-block-seams", 70, true, true, false);
                phase(test, world, "rotated-moving-solid-wall", 50, true, true, true);
            } else {
            String acceptedStatic = System.getProperty("port.smoke.physicalWalkingAcceptedStatic", "");
            if (acceptedStatic.isEmpty()) {
                phase(test, world, "static-standing", 40, false, false, false);
                phase(test, world, "static-forward-full-block-seams", 40, true, false, false);
                test.getInput().lookAt(180, 15);
                phase(test, world, "static-reverse-full-block-seams", 40, true, false, false);
            } else {
                Path previous = Path.of(acceptedStatic);
                byte[] bytes = Files.readAllBytes(previous);
                var prior = JsonParser.parseString(new String(bytes, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                var phases = new JsonArray();
                for (String name : List.of("static-standing", "static-forward-full-block-seams", "static-reverse-full-block-seams")) {
                    var found = prior.getAsJsonArray("phases").asList().stream()
                            .map(value -> value.getAsJsonObject())
                            .filter(value -> value.get("phase").getAsString().equals(name)
                                    && value.has("status") && value.get("status").getAsString().equals("passed"))
                            .findFirst().orElseThrow(() -> new AssertionError("Missing accepted static phase " + name));
                    phases.add(found.deepCopy());
                }
                result.put("retainedStaticAcceptance", Map.of("path", previous.toString(),
                        "sha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)),
                        "phases", phases, "replayed", false));
            }
            test.getInput().lookAt(0, 15);
            server.runOnServer(s -> movementStart = s.overworld().getGameTime());
            phase(test, world, "moving-standing", 40, false, true, false);
            phase(test, world, "moving-forward-full-block-slab-stairs", 100, true, true, false);
            phase(test, world, "moving-solid-wall-collision", 60, true, true, true);
            }
            var screenshot = test.takeScreenshot("physical-walking-complete");
            result.put("screenshot", screenshot.toString());
            checks.add(ROTATED
                    ? "only new native walking on actual Rapier platform at yaw17/pitch6 degrees: static and moving full-block seams plus solid-wall collision; original aligned six accepted phases retained without replay"
                    : "native SDL KeyEvents: actual player stands/walks/reverses over full-block seams, half slab and stairs on a static then translating Rapier platform; server/client trajectories, grounded tracking, physical wall and teleport counter checked");
            result.put("status", "passed");
            world.getServer().runOnServer(s -> s.saveEverything(false, true, true));
        } catch (Throwable failure) {
            result.put("status", "failed");
            result.put("failedPhase", active);
            result.put("error", failure.toString());
            throw failure instanceof Exception value ? value : new RuntimeException(failure);
        } finally {
            test.getInput().releaseKey(options -> options.keyUp);
            Files.writeString(PROOF, new GsonBuilder().setPrettyPrinting().create().toJson(result));
        }
    }

    private static void build(MinecraftServer server) {
        var level = server.overworld();
        List<BlockPos> blocks = new ArrayList<>();
        for (int z = -20; z <= 20; z++) for (int x = -6; x <= 6; x++) {
            var pos = ANCHOR.offset(x, 0, z);
            level.getChunk(pos);
            level.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
            level.setBlockAndUpdate(pos, Blocks.SMOOTH_STONE.defaultBlockState());
            blocks.add(pos);
            for (int y = 1; y <= 5; y++) level.setBlockAndUpdate(pos.above(y), Blocks.AIR.defaultBlockState());
        }
        // Ascending half slab, stair to full height, then a descending half slab.
        for (int x = -3; x <= 3; x++) {
            if (!ROTATED) {
            put(level, blocks, ANCHOR.offset(x, 1, -3), Blocks.SMOOTH_STONE_SLAB.defaultBlockState()
                    .setValue(SlabBlock.TYPE, SlabType.BOTTOM));
            put(level, blocks, ANCHOR.offset(x, 1, -2), Blocks.STONE_STAIRS.defaultBlockState()
                    .setValue(StairBlock.FACING, Direction.SOUTH));
            for (int z = -1; z <= 0; z++) put(level, blocks, ANCHOR.offset(x, 1, z), Blocks.SMOOTH_STONE.defaultBlockState());
            put(level, blocks, ANCHOR.offset(x, 1, 1), Blocks.SMOOTH_STONE_SLAB.defaultBlockState()
                    .setValue(SlabBlock.TYPE, SlabType.BOTTOM));
            }
            for (int y = 1; y <= 3; y++) put(level, blocks, ANCHOR.offset(x, y, 11), Blocks.STONE_BRICKS.defaultBlockState());
        }
        platform = SubLevelAssemblyHelper.assembleBlocks(level, ANCHOR, blocks,
                new BoundingBox3i(ANCHOR.offset(-6, 0, -20), ANCHOR.offset(6, 4, 20)));
        check(platform != null && !platform.isRemoved(), "Native platform assembly failed");
        localAnchor = platform.logicalPose().transformPositionInverse(new Vector3d(.5, 120.5, .5));
        for (BlockPos source : blocks) {
            check(level.getBlockState(source).isAir(), "Assembled source not cleared: " + source);
            var local = BlockPos.containing(platform.logicalPose().transformPositionInverse(Vec3.atCenterOf(source)));
            check(!level.getBlockState(local).isAir(), "Native assembled plot block missing: " + local);
        }
        check(Sable.HELPER.getContaining(level, localAnchor.x, localAnchor.z) == platform,
                "Assembled local anchor is not in actual platform plot");
    }

    private static void put(net.minecraft.server.level.ServerLevel level, List<BlockPos> blocks,
                            BlockPos position, BlockState state) {
        level.setBlockAndUpdate(position, state);
        blocks.add(position);
    }

    private static void attach(MinecraftServer server) {
        var axes = EnumSet.allOf(ConstraintJointAxis.class);
        axes.remove(ConstraintJointAxis.LINEAR_X);
        joint = SubLevelContainer.getContainer(server.overworld()).physicsSystem().getPipeline()
                .addConstraint(null, platform, new GenericConstraintConfiguration(
                        new Vector3d(.5, 120.5, .5), new Vector3d(localAnchor),
                        new Quaterniond(GUIDE_ORIENTATION), new Quaterniond(), axes));
        motor(0);
        check(joint.isValid(), "Native platform guide joint invalid");
    }

    private static void motor(double target) {
        double mass = platform.getMassTracker().getMass();
        joint.setMotor(ConstraintJointAxis.LINEAR_X, target, mass * 40, mass * 12, false, 0);
    }

    private static void phase(ClientGameTestContext test, TestSingleplayerContext world, String name,
                              int ticks, boolean walking, boolean moving, boolean wall) throws Exception {
        active = name;
        if (walking) test.getInput().holdKey(options -> options.keyUp);
        else test.getInput().releaseKey(options -> options.keyUp);
        List<Map<String, Object>> rows = new ArrayList<>();
        try {
            for (int i = 0; i < ticks; i++) {
                world.getServer().runOnServer(server -> {
                    check(joint.isValid() && !platform.isRemoved(), "Native platform or guide joint vanished");
                    if (moving) {
                        motor((server.overworld().getGameTime() - movementStart) * .05);
                        // Updating a Rapier joint target does not wake a settled body.
                        SubLevelContainer.getContainer(server.overworld()).physicsSystem().getPipeline().wakeUp(platform);
                    }
                });
                pace(test);
                Map<String, Object> client = test.computeOnClient(mc -> {
                    var sub = SubLevelContainer.getContainer(mc.level).getSubLevel(platform.getUniqueId());
                    var row = sample(mc.player, sub);
                    if (ROTATED) {
                        var camera = mc.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
                        row.put("renderCameraWorld", List.of(camera.pos.x, camera.pos.y, camera.pos.z));
                        row.put("renderCameraPartialTick", camera.cameraEntityPartialTicks);
                        var eye = mc.player.getEyePosition();
                        row.put("actualPlayerEye", List.of(eye.x, eye.y, eye.z));
                        row.put("renderCameraToActualEyeDistance", camera.pos.distanceTo(eye));
                        if (sub instanceof ClientSubLevel clientSub) {
                            var localCamera = clientSub.renderPose(camera.cameraEntityPartialTicks).transformPositionInverse(camera.pos);
                            row.put("renderCameraRelativePlot", List.of(localCamera.x - localAnchor.x,
                                    localCamera.y - localAnchor.y, localCamera.z - localAnchor.z));
                        }
                    }
                    return row;
                });
                AtomicReference<Map<String, Object>> observed = new AtomicReference<>();
                world.getServer().runOnServer(server -> {
                    var row = sample(server.getPlayerList().getPlayers().getFirst(), platform);
                    row.put("teleportId", teleportId(server));
                    row.put("bodyVelocity", vector(RigidBodyHandle.of(platform).getLinearVelocity(new Vector3d())));
                    row.put("bodyPosition", vector(platform.logicalPose().position()));
                    var orientation = platform.logicalPose().orientation();
                    row.put("bodyOrientation", List.of(orientation.x(), orientation.y(), orientation.z(), orientation.w()));
                    observed.set(row);
                });
                Map<String, Object> pair = new LinkedHashMap<>();
                pair.put("phase", name); pair.put("sample", i); pair.put("client", client); pair.put("server", observed.get());
                SAMPLES.add(pair); rows.add(pair);
                if (i % 20 == 0) System.out.println("PHYSICAL WALK " + pair);
                // Collect the full phase before asserting so a failure retains the actual trajectory.
            }
        } finally {
            test.getInput().releaseKey(options -> options.keyUp);
        }
        var stats = new LinkedHashMap<String, Object>();
        stats.put("phase", name); stats.put("samples", rows.size());
        stats.put("walking", walking); stats.put("moving", moving); stats.put("wall", wall);
        PHASES.add(stats);
        double startZ = coordinate(rows.getFirst(), "client", "relativePlot", 2);
        double endZ = coordinate(rows.getLast(), "client", "relativePlot", 2);
        double platformTravel = Math.abs(coordinate(rows.getLast(), "server", "bodyPosition", 0)
                - coordinate(rows.getFirst(), "server", "bodyPosition", 0));
        double maximumStep = 0, maxVerticalStep = 0, maximumLocalDisagreement = 0;
        int tracked = 0, grounded = 0, longestStall = 0, stall = 0;
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            Map<?, ?> c = (Map<?, ?>) row.get("client"), s = (Map<?, ?>) row.get("server");
            check(((Number) s.get("teleportId")).intValue() == baselineTeleportId,
                    name + ": server issued movement correction, native teleport counter=" + s.get("teleportId"));
            if (platform.getUniqueId().toString().equals(c.get("tracking"))) tracked++;
            if (Boolean.TRUE.equals(c.get("onGround"))) grounded++;
            double error = Math.hypot(coordinate(row, "client", "relativePlot", 0) - coordinate(row, "server", "relativePlot", 0),
                    coordinate(row, "client", "relativePlot", 2) - coordinate(row, "server", "relativePlot", 2));
            maximumLocalDisagreement = Math.max(maximumLocalDisagreement, error);
            if (i == 0) continue;
            Map<String, Object> prior = rows.get(i - 1);
            long dt = ((Number) c.get("gameTime")).longValue()
                    - ((Number) ((Map<?, ?>) prior.get("client")).get("gameTime")).longValue();
            if (dt <= 0) continue;
            double dz = Math.abs(coordinate(row, "client", "relativePlot", 2) - coordinate(prior, "client", "relativePlot", 2));
            double dx = Math.abs(coordinate(row, "client", "relativePlot", 0) - coordinate(prior, "client", "relativePlot", 0));
            maximumStep = Math.max(maximumStep, Math.hypot(dx, dz) / dt);
            maxVerticalStep = Math.max(maxVerticalStep, Math.abs(coordinate(row, "client", "relativePlot", 1)
                    - coordinate(prior, "client", "relativePlot", 1)) / dt);
            if (walking && !wall && i > 5 && dz / dt < .025) stall += dt;
            else stall = 0;
            longestStall = Math.max(longestStall, stall);
        }
        stats.put("plotZTravel", endZ - startZ); stats.put("platformTravel", platformTravel);
        stats.put("maximumHorizontalStepPerTick", maximumStep); stats.put("maximumVerticalStepPerTick", maxVerticalStep);
        stats.put("maximumClientServerLocalDifference", maximumLocalDisagreement);
        stats.put("trackedSamples", tracked); stats.put("groundedSamples", grounded); stats.put("longestUnexpectedStallTicks", longestStall);
        check(tracked >= ticks * .8, name + ": lost physical platform tracking in " + (ticks - tracked) + " samples");
        check(grounded >= ticks * .75, name + ": lost physical support in " + (ticks - grounded) + " samples");
        check(maximumStep < .65 && maxVerticalStep < .85, name + ": discontinuous player trajectory: " + stats);
        check(maximumLocalDisagreement < 1.0, name + ": server/client plot positions diverged: " + stats);
        if (moving) check(platformTravel > ticks * .02, name + ": real joint motor did not move platform: " + platformTravel);
        if (!walking) check(Math.abs(endZ - startZ) < .15, name + ": stationary player slid in platform space");
        if (walking && !wall) {
            check(Math.abs(endZ - startZ) > ticks * .12, name + ": player did not walk far enough: " + stats);
            check(longestStall <= 2, name + ": player stalled while native input held: " + stats);
        }
        if (wall) {
            check(endZ > 9.5 && endZ < 10.25, name + ": solid native wall failed to stop player: " + stats);
            check(Math.abs(endZ - coordinate(rows.get(ticks - 15), "client", "relativePlot", 2)) < .1,
                    name + ": player did not remain blocked by real solid wall");
        }
        stats.put("status", "passed");
    }

    private static Map<String, Object> sample(Entity player, SubLevel sub) {
        check(sub != null && !sub.isRemoved(), "Client/server platform missing");
        Map<String, Object> row = new LinkedHashMap<>();
        var local = sub.logicalPose().transformPositionInverse(player.position());
        row.put("gameTime", player.level().getGameTime()); row.put("playerTick", player.tickCount);
        row.put("world", List.of(player.getX(), player.getY(), player.getZ()));
        row.put("relativePlot", List.of(local.x - localAnchor.x, local.y - localAnchor.y, local.z - localAnchor.z));
        var track = Sable.HELPER.getTrackingSubLevel(player);
        row.put("tracking", track == null ? "none" : track.getUniqueId().toString());
        row.put("onGround", player.onGround()); row.put("horizontalCollision", player.horizontalCollision);
        row.put("verticalCollisionBelow", player.verticalCollisionBelow);
        row.put("deltaMovement", List.of(player.getDeltaMovement().x, player.getDeltaMovement().y, player.getDeltaMovement().z));
        var collision = ((EntityMovementExtension) player).sable$getCollisionInfo();
        if (collision != null) {
            row.put("subLevelHorizontalCollision", collision.subLevelHorizontalCollision);
            if (collision.inheritedMotion != null) row.put("inheritedMotion", List.of(collision.inheritedMotion.x, collision.inheritedMotion.y, collision.inheritedMotion.z));
        }
        return row;
    }

    private static void retainAlignedAcceptance(Map<String, Object> result) throws Exception {
        Path path = Path.of(System.getProperty("port.smoke.physicalWalkingAlignedProof",
                PROOF.getParent().getParent().getParent()
                        .resolve("physical-walking-r6-moving/client/physical-walking.json").toString()));
        byte[] bytes = Files.readAllBytes(path);
        var prior = JsonParser.parseString(new String(bytes, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        check(prior.get("status").getAsString().equals("passed"), "Aligned moving proof did not pass");
        for (String name : List.of("moving-standing", "moving-forward-full-block-slab-stairs", "moving-solid-wall-collision")) {
            check(prior.getAsJsonArray("phases").asList().stream().map(value -> value.getAsJsonObject())
                    .anyMatch(value -> value.get("phase").getAsString().equals(name)
                            && value.has("status") && value.get("status").getAsString().equals("passed")),
                    "Missing accepted aligned moving phase " + name);
        }
        var staticAcceptance = prior.getAsJsonObject("retainedStaticAcceptance");
        Path originalStatic = Path.of(staticAcceptance.get("path").getAsString());
        check(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(originalStatic)))
                .equals(staticAcceptance.get("sha256").getAsString()), "Original accepted static proof changed");
        result.put("retainedAlignedAcceptance", Map.of("path", path.toString(),
                "sha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)),
                "movingPhases", prior.getAsJsonArray("phases").deepCopy(),
                "staticAcceptance", staticAcceptance.deepCopy(), "replayed", false));
    }

    private static double coordinate(Map<String, Object> pair, String side, String property, int axis) {
        return ((Number) ((List<?>) ((Map<?, ?>) pair.get(side)).get(property)).get(axis)).doubleValue();
    }
    private static List<Double> vector(org.joml.Vector3dc value) { return List.of(value.x(), value.y(), value.z()); }
    private static int teleportId(MinecraftServer server) {
        try { return TELEPORT_ID.getInt(server.getPlayerList().getPlayers().getFirst().connection); }
        catch (IllegalAccessException e) { throw new RuntimeException(e); }
    }
    private static Field field(Class<?> owner, String name) {
        try { var f = owner.getDeclaredField(name); f.setAccessible(true); return f; }
        catch (Exception failure) { throw new RuntimeException(failure); }
    }
    private static void pace(ClientGameTestContext test) throws InterruptedException { test.waitTick(); Thread.sleep(40); }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private PackagedPhysicalWalkingRegression() {}
}
