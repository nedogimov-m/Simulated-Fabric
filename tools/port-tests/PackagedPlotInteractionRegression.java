import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Actual native clicks and asynchronous block updates on a moved, rotated plot. */
public final class PackagedPlotInteractionRegression {
    private static final BlockPos SOURCE = new BlockPos(16, 124, 16);
    private static ServerSubLevel plot;
    private static BlockPos lamp, lever, change;
    private static String phase = "assembly";

    public static void run(ClientGameTestContext test, TestSingleplayerContext world, List<String> checks) throws Exception {
        var report = new LinkedHashMap<String, Object>();
        var rows = new ArrayList<Map<String, Object>>(); report.put("phases", rows);
        report.put("scope", "One genuinely assembled plot translated and rotated in native Rapier; actual client ray and right mouse clicks turn a lever ON then OFF. An adjacent lamp outside the clicked/adjacent direct correction positions must follow both server changes. Server replacement/removal must reach the client and its raycast; original world cells remain air.");
        var path = Path.of(System.getProperty("port.smoke.report")).resolveSibling("plot-interaction.json");
        try {
            var server = world.getServer();
            server.runCommand("gamemode creative @a"); server.runCommand("time set noon");
            server.runCommand("weather clear"); server.runCommand("tp @a 0 150 0");
            server.runOnServer(PackagedPlotInteractionRegression::build);
            server.waitFor(s -> plot.getMassTracker().getMass() > 0 && RigidBodyHandle.of(plot) != null, 200);
            server.runOnServer(s -> {
                var rotation = new Quaterniond().rotateY(Math.toRadians(31)).rotateZ(Math.toRadians(13));
                var position = new Vector3d(plot.logicalPose().position()).add(5, 3, 4);
                RigidBodyHandle.of(plot).teleport(position, rotation);
                var joint = SubLevelContainer.getContainer(s.overworld()).physicsSystem().getPipeline().addConstraint(null, plot,
                        new FixedConstraintConfiguration(position, new Vector3d(plot.getMassTracker().getCenterOfMass()), rotation));
                check(joint != null && joint.isValid(), "Native fixed joint missing");
            });
            test.waitFor(c -> c.level != null && SubLevelContainer.getContainer(c.level).getSubLevel(plot.getUniqueId())
                    instanceof ClientSubLevel sub && sub.isFinalized(), 400);
            test.waitTicks(20);
            server.runOnServer(s -> {
                check(s.overworld().getBlockState(lever).is(Blocks.LEVER), "Lever missing before native clicks");
                check(s.overworld().getBlockState(change).is(Blocks.STONE), "Connected replacement cell missing before native clicks");
            });
            report.put("plot", plot.getUniqueId().toString());
            report.put("plotLever", lever.toShortString()); report.put("plotLamp", lamp.toShortString());
            report.put("plotReplacementCell", change.toShortString());
            server.runOnServer(s -> {
                var p = s.getPlayerList().getPlayers().getFirst(); p.getAbilities().flying = true; p.onUpdateAbilities();
                p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                p.inventoryMenu.broadcastFullState();
                report.put("serverPose", List.of(plot.logicalPose().position().x(), plot.logicalPose().position().y(), plot.logicalPose().position().z()));
                var q = plot.logicalPose().orientation(); report.put("serverOrientation", List.of(q.x(), q.y(), q.z(), q.w()));
            });
            aim(test, world, lever, .15);
            for (boolean on : new boolean[]{true, false}) {
                phase = on ? "native-lever-on" : "native-lever-off";
                test.waitFor(c -> isHit(c, lever), 120);
                var row = new LinkedHashMap<String, Object>(); row.put("phase", phase);
                test.runOnClient(c -> {
                    var hit = (BlockHitResult)c.hitResult;
                    check(hit.getDirection() != Direction.DOWN && !lever.relative(hit.getDirection()).equals(lamp),
                            "Lamp must be outside native clicked/adjacent correction packets");
                    row.put("nativeRay", ray(hit));
                });
                test.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);
                server.waitFor(s -> s.overworld().getBlockState(lever).getValue(LeverBlock.POWERED) == on
                        && s.overworld().getBlockState(lamp).getValue(RedstoneLampBlock.LIT) == on, 120);
                test.waitFor(c -> c.level.getBlockState(lever).getValue(LeverBlock.POWERED) == on
                        && c.level.getBlockState(lamp).getValue(RedstoneLampBlock.LIT) == on, 120);
                row.put("state", snapshot(test, world)); row.put("status", "passed"); rows.add(row);
            }
            phase = "server-replace";
            server.runOnServer(s -> check(s.overworld().setBlockAndUpdate(change, Blocks.EMERALD_BLOCK.defaultBlockState()), "Replacement failed"));
            test.waitFor(c -> c.level.getBlockState(change).is(Blocks.EMERALD_BLOCK), 120);
            aim(test, world, change, .5); test.waitFor(c -> isHit(c, change), 120);
            rows.add(Map.of("phase", phase, "state", snapshot(test, world), "nativeRay", test.computeOnClient(c -> ray(c.hitResult)), "status", "passed"));
            phase = "server-remove";
            server.runOnServer(s -> check(s.overworld().setBlockAndUpdate(change, Blocks.AIR.defaultBlockState()), "Removal failed"));
            test.waitFor(c -> c.level.getBlockState(change).isAir() && !isHit(c, change), 120);
            test.runOnClient(c -> {
                for (var source : List.of(SOURCE, SOURCE.above(), SOURCE.east())) check(c.level.getBlockState(source).isAir(), "Client retains original assembly source: " + source);
            });
            rows.add(Map.of("phase", phase, "state", snapshot(test, world), "nativeRay", test.computeOnClient(c -> ray(c.hitResult)), "status", "passed"));
            report.put("screenshot", test.takeScreenshot("plot-interaction-after-removal").toString());
            report.put("status", "passed");
            checks.add("translated/rotated native plot: real client lever ON/OFF; neighbouring lamp broadcasts; server replacement/removal converge and removed block no longer raycasts");
        } catch (Throwable failure) {
            report.put("status", "failed"); report.put("failedPhase", phase); report.put("error", failure.toString());
            try { report.put("failureState", snapshot(test, world)); } catch (Throwable ignored) { }
            throw failure;
        } finally { Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(report)); }
    }

    private static void build(MinecraftServer server) {
        var level = server.overworld(); var sourceLever = SOURCE.above(); var sourceChange = SOURCE.east();
        for (var p : List.of(SOURCE, sourceLever, sourceChange)) level.getChunk(p);
        level.setBlockAndUpdate(SOURCE, Blocks.REDSTONE_LAMP.defaultBlockState());
        level.setBlockAndUpdate(sourceLever, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR).setValue(LeverBlock.FACING, Direction.NORTH));
        level.setBlockAndUpdate(sourceChange, Blocks.STONE.defaultBlockState());
        plot = SubLevelAssemblyHelper.assembleBlocks(level, SOURCE, List.of(SOURCE, sourceLever, sourceChange), new BoundingBox3i(SOURCE, sourceChange.above()));
        check(plot != null && !plot.isRemoved(), "Native assembly failed");
        lamp = BlockPos.containing(plot.logicalPose().transformPositionInverse(Vec3.atCenterOf(SOURCE)));
        lever = BlockPos.containing(plot.logicalPose().transformPositionInverse(Vec3.atCenterOf(sourceLever)));
        change = BlockPos.containing(plot.logicalPose().transformPositionInverse(Vec3.atCenterOf(sourceChange)));
        for (var p : List.of(SOURCE, sourceLever, sourceChange)) check(level.getBlockState(p).isAir(), "Original source remains " + p);
    }

    private static void aim(ClientGameTestContext test, TestSingleplayerContext world, BlockPos block, double height) {
        var localTarget = new Vec3(block.getX() + .5, block.getY() + height, block.getZ() + .5);
        var eye = world.getServer().computeOnServer(s -> plot.logicalPose().transformPosition(localTarget.add(0, .8, -2.0)));
        world.getServer().runCommand(String.format(Locale.ROOT, "tp @a %.8f %.8f %.8f", eye.x, eye.y - 1.62, eye.z));
        world.getServer().waitFor(s -> pendingPosition(s.getPlayerList().getPlayers().getFirst().connection), 120);
        test.runOnClient(c -> c.gui.setScreen(null));
        var angle = test.computeOnClient(c -> {
            var sub = (ClientSubLevel)SubLevelContainer.getContainer(c.level).getSubLevel(plot.getUniqueId());
            var d = sub.renderPose().transformPosition(localTarget).subtract(c.player.getEyePosition());
            return new Vec2((float)Math.toDegrees(Math.atan2(-d.x, d.z)), (float)-Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z))));
        });
        test.getInput().lookAt(angle.x, angle.y);
    }

    private static Map<String, Object> snapshot(ClientGameTestContext test, TestSingleplayerContext world) {
        var server = world.getServer().computeOnServer(s -> Map.of("lever", s.overworld().getBlockState(lever).toString(), "lamp", s.overworld().getBlockState(lamp).toString(), "replacement", s.overworld().getBlockState(change).toString(), "inNativeRange", s.getPlayerList().getPlayers().getFirst().isWithinBlockInteractionRange(lever, 1)));
        var client = test.computeOnClient(c -> Map.of("lever", c.level.getBlockState(lever).toString(), "lamp", c.level.getBlockState(lamp).toString(), "replacement", c.level.getBlockState(change).toString(), "nativeRay", ray(c.hitResult)));
        return Map.of("server", server, "client", client);
    }
    private static boolean isHit(Minecraft c, BlockPos pos) { return c.hitResult instanceof BlockHitResult h && h.getType() == HitResult.Type.BLOCK && h.getBlockPos().equals(pos); }
    private static Map<String, Object> ray(HitResult hit) {
        if (hit == null) return Map.of("type", "null");
        return hit instanceof BlockHitResult b ? Map.of("type", hit.getType().toString(), "block", b.getBlockPos().toShortString(), "face", b.getDirection().toString(), "location", hit.getLocation().toString()) : Map.of("type", hit.getType().toString(), "location", hit.getLocation().toString());
    }
    private static boolean pendingPosition(Object connection) {
        try { var f = connection.getClass().getDeclaredField("awaitingPositionFromClient"); f.setAccessible(true); return f.get(connection) == null; }
        catch (ReflectiveOperationException failure) { throw new RuntimeException(failure); }
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private PackagedPlotInteractionRegression() { }
}
