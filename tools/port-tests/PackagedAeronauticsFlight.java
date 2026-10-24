import com.google.gson.GsonBuilder;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.BasePropellerBlockEntity;
import dev.eriksonn.aeronautics.index.AeroBlocks;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.render.vanilla.VanillaChunkedSubLevelRenderData;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlockEntity;
import dev.simulated_team.simulated.index.SimBlocks;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/** Visible flight: genuine assembler, Create motor networks and Aero propeller forces. */
public final class PackagedAeronauticsFlight {
    private static final int DECK = 120;
    private static final BlockPos ASSEMBLER = new BlockPos(0, DECK + 1, 0);
    private static final List<BlockPos> LIFT = new ArrayList<>(), DRIVE = new ArrayList<>();
    private static final LinkedHashMap<BlockPos, BlockState> BLOCKS = new LinkedHashMap<>();
    private static final LinkedHashMap<BlockPos, BlockPos> MOVED = new LinkedHashMap<>();
    private static final List<Object> SAMPLES = new ArrayList<>();
    private static ServerSubLevel ship;
    private static Vector3d start, geometricCenter;
    private static double targetY, maximumLift, maximumTravel, maximumTilt;
    private static int activeLiftTicks, activeDriveTicks;
    private static final Path PROOF = Path.of(System.getProperty("port.smoke.report")).resolveSibling("flight-proof.json");
    private static final Path STOP = PROOF.resolveSibling("stop-viewing");

    public static void run(ClientGameTestContext test, TestSingleplayerContext world, List<String> checks) throws Exception {
        STOP.toFile().delete();
        var server = world.getServer();
        server.runCommand("gamemode spectator @a");
        server.runCommand("time set noon");
        server.runCommand("weather clear");
        server.runCommand("tp @a 16 130 19 140 22");
        server.runOnServer(PackagedAeronauticsFlight::build);
        pace(test, 30);
        server.runOnServer(s -> {
            for (var p : LIFT) motor(s, p).generatedSpeed.setValue(0);
            for (var p : DRIVE) motor(s, p).generatedSpeed.setValue(0);
            s.getPlayerList().getPlayers().getFirst().sendSystemMessage(Component.literal("AERONAUTICS: корабль построен. Запускаю сборку и винты..."), false);
        });
        pace(test, 30);
        var before = test.takeScreenshot("aeronautics-before-launch");
        server.runOnServer(s -> {
            var level = s.overworld();
            var controller = (PhysicsAssemblerBlockEntity) level.getBlockEntity(ASSEMBLER);
            check(controller != null, "Real physics assembler was not created");
            controller.assembleOrDisassemble();
            for (var candidate : SubLevelContainer.getContainer(level).getAllSubLevels())
                if (candidate instanceof ServerSubLevel value && !value.isRemoved()) { check(ship == null, "Unexpected second ship"); ship = value; }
            check(ship != null, "Actual assembler did not allocate a Sable ship: " + controller.getLastAssemblyException());
            geometricCenter = ship.logicalPose().transformPositionInverse(new Vector3d(.5, DECK + 1.5, .5));
            for (var entry : BLOCKS.entrySet()) {
                check(level.getBlockState(entry.getKey()).isAir(), "Source block was not transferred: " + entry.getKey());
                check(level.getBlockState(local(entry.getKey())).is(entry.getValue().getBlock()), "Transferred ship block changed: " + entry.getKey());
            }
            check(((PhysicsAssemblerBlockEntity) level.getBlockEntity(local(ASSEMBLER))).isPrimaryAssembler(), "Moved assembler lost primary ownership");
        });
        server.waitFor(s -> ship.getSelfMassTracker().getMass() > 0 && RigidBodyHandle.of(ship) != null, 200);
        test.waitFor(client -> {
            var container = SubLevelContainer.getContainer(client.level);
            if (!(container.getSubLevel(ship.getUniqueId()) instanceof ClientSubLevel value) || !value.isFinalized()) return false;
            var camera = client.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
            return value.getRenderData() instanceof VanillaChunkedSubLevelRenderData mesh && !mesh.extract(camera.pos, camera.cameraEntityPartialTicks).isEmpty();
        }, 600);
        server.runOnServer(s -> {
            start = new Vector3d(ship.logicalPose().position()); targetY = start.y + 12;
            double pressure = DimensionPhysicsData.getAirPressure(s.overworld(), start);
            double gravity = Math.abs(DimensionPhysicsData.getGravity(s.overworld(), start).y);
            check(4 * 256 * pressure > ship.getMassTracker().getMass() * gravity * 1.15,
                    "Ship is too heavy for its actual four lift propellers");
        });
        for (int tick = 0; tick < 440; tick++) {
            final int phaseTick = tick;
            server.runOnServer(s -> control(s, phaseTick, true));
            test.waitTick();
            Thread.sleep(40);
            if (tick == 180) test.takeScreenshot("aeronautics-takeoff");
        }
        var flying = test.takeScreenshot("aeronautics-in-flight");
        check(maximumLift >= 6, "Real propellers did not lift the ship six blocks: " + maximumLift);
        check(maximumTravel >= 10, "Real drive propellers did not move the ship ten blocks: " + maximumTravel);
        check(activeLiftTicks >= 100 && activeDriveTicks >= 100, "Native motor/propeller activity was not sustained");
        check(maximumTilt < 35, "Ship became unstable: tilt=" + maximumTilt);
        checks.add("actual Aeronautics ship: " + BLOCKS.size() + " glued blocks, real primary physics assembler, four lift/two drive propellers powered through real Create creative motor networks; 440 ticks of native Rapier flight, rise=" + maximumLift + ", travel=" + maximumTravel + ", max tilt=" + maximumTilt);
        var proof = new LinkedHashMap<String, Object>();
        proof.put("status", "passed"); proof.put("scope", "Actual assembled Aeronautics propeller flight; creative motors provide power and a test autopilot adjusts their ordinary speed settings. No ship teleport, velocity, impulse or constraint writes.");
        proof.put("blocks", BLOCKS.size()); proof.put("ship", ship.getUniqueId().toString());
        proof.put("rise", maximumLift); proof.put("travel", maximumTravel); proof.put("maximumTiltDegrees", maximumTilt);
        proof.put("ticks", 440); proof.put("samples", SAMPLES); proof.put("screenshots", List.of(before.toString(), flying.toString()));
        proof.put("world", world.getWorldSave().getSaveDirectory().toString());
        proof.put("liveViewing", Boolean.getBoolean("port.smoke.keepOpen"));
        Files.writeString(PROOF, new GsonBuilder().setPrettyPrinting().create().toJson(proof));
        server.runOnServer(s -> {
            s.saveEverything(false, true, true);
            s.getPlayerList().getPlayers().getFirst().sendSystemMessage(Component.literal("AERONAUTICS FLIGHT PASS — корабль летит. Камера следует рядом."), false);
        });
        System.out.println("AERONAUTICS FLIGHT PASS: " + PROOF + "; keeping visible world=" + Boolean.getBoolean("port.smoke.keepOpen"));
        int tick = 440;
        while (Boolean.getBoolean("port.smoke.keepOpen") && !Files.exists(STOP)) {
            final int phaseTick = tick++;
            server.runOnServer(s -> control(s, phaseTick, false));
            test.waitTick(); Thread.sleep(45);
        }
    }

    private static void build(MinecraftServer server) {
        var level = server.overworld();
        for (int z = -1; z <= 1; z++) for (int x = -1; x <= 1; x++) level.setChunkForced(x, z, true);
        for (int z = -10; z <= 10; z++) for (int x = -10; x <= 10; x++) {
            level.setBlock(new BlockPos(x, DECK - 1, z), (Math.abs(x) == 10 || Math.abs(z) == 10 ? Blocks.CONCRETE.pick(DyeColor.YELLOW) : Blocks.POLISHED_DEEPSLATE).defaultBlockState(), 3);
            for (int y = DECK; y <= DECK + 6; y++) level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
        }
        for (int z = -3; z <= 3; z++) for (int x = -1; x <= 1; x++) put(x, 0, z, Blocks.SPRUCE_PLANKS.defaultBlockState());
        put(0, 0, -4, Blocks.SPRUCE_PLANKS.defaultBlockState());
        for (int x : new int[]{-3, 3}) for (int z : new int[]{-2, 2}) {
            put(Integer.signum(x) * 2, 0, z, Blocks.SPRUCE_PLANKS.defaultBlockState());
            var motor = put(x, 0, z, AllBlocks.CREATIVE_MOTOR.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP)); LIFT.add(motor);
            put(x, 1, z, AeroBlocks.ANDESITE_PROPELLER.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));
        }
        for (int x : new int[]{-1, 1}) {
            DRIVE.add(put(x, 1, 3, AllBlocks.CREATIVE_MOTOR.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.SOUTH)));
            put(x, 1, 4, AeroBlocks.ANDESITE_PROPELLER.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.SOUTH));
            for (int z = -2; z <= 1; z++) put(x, 1, z, Blocks.SPRUCE_FENCE.defaultBlockState());
        }
        put(0, 1, 0, SimBlocks.PHYSICS_ASSEMBLER.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR).setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        put(0, 1, 2, Blocks.OAK_FENCE.defaultBlockState()); put(0, 2, 2, Blocks.WOOL.pick(DyeColor.YELLOW).defaultBlockState());
        for (var e : BLOCKS.entrySet()) { level.getChunk(e.getKey()); level.setBlock(e.getKey(), e.getValue(), 3); }
        level.addFreshEntity(new SuperGlueEntity(level, new AABB(-3, DECK, -4, 4, DECK + 3, 5)));
    }

    private static BlockPos put(int x, int y, int z, BlockState state) { var p = new BlockPos(x, DECK + y, z); BLOCKS.put(p, state); return p; }
    private static BlockPos local(BlockPos original) { return MOVED.computeIfAbsent(original, p -> BlockPos.containing(ship.logicalPose().transformPositionInverse(Vec3.atCenterOf(p)))); }
    private static CreativeMotorBlockEntity motor(MinecraftServer s, BlockPos original) { return (CreativeMotorBlockEntity) s.overworld().getBlockEntity(ship == null ? original : local(original)); }
    private static BasePropellerBlockEntity prop(MinecraftServer s, BlockPos motor, Direction direction) { return (BasePropellerBlockEntity) s.overworld().getBlockEntity(local(motor.relative(direction))); }

    private static void control(MinecraftServer server, int tick, boolean record) {
        check(!ship.isRemoved(), "Ship was removed during real flight");
        var handle = RigidBodyHandle.of(ship); var velocity = handle.getLinearVelocity(new Vector3d());
        var angular = handle.getAngularVelocity(new Vector3d()); var position = new Vector3d(ship.logicalPose().position());
        var orientation = ship.logicalPose().orientation(); var up = orientation.transform(new Vector3d(0, 1, 0));
        check(velocity.isFinite() && position.isFinite(), "Native flight pose/velocity is not finite");
        var center = ship.getMassTracker().getCenterOfMass();
        var centerWorld = ship.logicalPose().transformPosition(new Vector3d(center));
        double mass = ship.getMassTracker().getMass(), pressure = DimensionPhysicsData.getAirPressure(server.overworld(), centerWorld);
        double gravity = Math.abs(DimensionPhysicsData.getGravity(server.overworld(), centerWorld).y);
        double acceleration = Math.clamp((targetY - position.y) * .6 - velocity.y * 1.8, -2, 3);
        double total = mass * (gravity + acceleration) / Math.max(.5, up.y);
        var torque = new Vector3d(up).cross(0, 1, 0).mul(mass * 10).sub(new Vector3d(angular).mul(mass * 6));
        orientation.transformInverse(torque);
        double thrustSum = 0; boolean liftActive = true, driveActive = true;
        for (var p : LIFT) {
            // Balance around actual COM, including unequal hull/engine mass. Only
            // real motor dial settings are controlled; Aero supplies every force.
            double originalX = p.getX(), originalZ = p.getZ();
            double cx = center.x() - geometricCenter.x, cz = center.z() - geometricCenter.z;
            double force = total / 4 + originalX * (cx * total + torque.z) / 36 + originalZ * (cz * total - torque.x) / 16;
            var localVelocity = orientation.transformInverse(new Vector3d(velocity));
            int rpm = (int) Math.round(-Math.clamp(force / pressure + 10 * localVelocity.y, 0, 256));
            motor(server, p).generatedSpeed.setValue(rpm);
            var propeller = prop(server, p, Direction.UP); thrustSum += propeller.getScaledThrust();
            liftActive &= propeller.getSpeed() < -5 && propeller.isActive();
        }
        double drive = tick < 100 ? 0 : Math.clamp(mass * (2.5 + velocity.z) * .7, 0, 90);
        for (var p : DRIVE) {
            int rpm = (int) Math.round(Math.clamp(drive / (2 * pressure) - 10 * velocity.z, 0, 128));
            motor(server, p).generatedSpeed.setValue(rpm);
            var propeller = prop(server, p, Direction.SOUTH);
            driveActive &= propeller.getSpeed() > 5 && propeller.isActive();
        }
        maximumLift = Math.max(maximumLift, position.y - start.y);
        maximumTravel = Math.max(maximumTravel, Math.hypot(position.x - start.x, position.z - start.z));
        double tilt = Math.toDegrees(Math.acos(Math.clamp(up.y, -1, 1))); maximumTilt = Math.max(maximumTilt, tilt);
        if (liftActive) activeLiftTicks++; if (driveActive) activeDriveTicks++;
        if (tick % 20 == 0) {
            double px = centerWorld.x + 15, py = centerWorld.y + 9, pz = centerWorld.z + 18;
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), String.format(Locale.ROOT, "tp @a %.5f %.5f %.5f 140 21", px, py, pz));
            server.getPlayerList().getPlayers().getFirst().sendOverlayMessage(Component.literal(String.format(Locale.ROOT, "AERONAUTICS | высота +%.1f м | путь %.1f м | скорость %.1f м/с | %s", position.y - start.y, maximumTravel, velocity.length(), tick < 100 ? "ВЗЛЁТ" : "ПОЛЁТ")));
            if (record) {
                var sample = new LinkedHashMap<String, Object>(); sample.put("tick", tick); sample.put("position", List.of(position.x, position.y, position.z));
                sample.put("velocity", List.of(velocity.x, velocity.y, velocity.z)); sample.put("mass", mass); sample.put("pressure", pressure);
                sample.put("actualLiftThrust", thrustSum); sample.put("tiltDegrees", tilt); SAMPLES.add(sample);
                System.out.println("AERO FLIGHT " + sample);
            }
        }
    }
    private static void pace(ClientGameTestContext test, int ticks) throws InterruptedException { for (int i = 0; i < ticks; i++) { test.waitTick(); Thread.sleep(40); } }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
