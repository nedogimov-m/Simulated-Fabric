import com.google.gson.GsonBuilder;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import dev.ryanhcode.offroad.index.OffroadBlocks;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelBlockEntity;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.index.SimStats;
import dev.simulated_team.simulated.network.packets.AssemblePacket;
import dev.simulated_team.simulated.network.packets.SteeringWheelPacket;
import net.createmod.ponder.foundation.registration.PonderSceneRegistry;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/** Actual wire packets/stat awards and the reported Offroad Ponder failure. */
public final class PackagedCrashRegressions {
    private static final BlockPos WHEEL = new BlockPos(16, 121, 16), ASSEMBLER = WHEEL.east(2);
    private static final Path REPORT = Path.of(System.getProperty("port.smoke.report")).resolveSibling("crash-regressions.json");

    public static void run(ClientGameTestContext test, TestSingleplayerContext world, List<String> checks) throws Exception {
        var result = new LinkedHashMap<String, Object>();
        var registered = new ArrayList<String>();
        var server = world.getServer();
        if (!System.getProperty("port.smoke.crashChecks", "all").equals("ponder")) {
        server.runCommand("gamemode creative @a");
        server.runCommand("tp @a 16 122 19 180 35");
        int[] before = server.computeOnServer(s -> {
            for (var f : SimStats.class.getFields()) if (f.getType() == SimStats.Stat.class) {
                var stat = (SimStats.Stat) f.get(null); check(stat != null, "Uninitialized custom statistic: " + f.getName());
                var id = stat.identifier().get(); check(BuiltInRegistries.CUSTOM_STAT.containsKey(id), "Unregistered native custom stat: " + id);
                check(Stats.CUSTOM.contains(id), "Native stat formatter was not registered: " + id);
                check(Stats.CUSTOM.get(id).format(35000).equals(stat.formatter().format(35000)), "Custom stat formatter changed: " + id);
                registered.add(id.toString());
            }
            check(registered.size() == 7, "Expected all seven real Sim custom statistics");
            var level = s.overworld(); level.getChunk(WHEEL); level.getChunk(ASSEMBLER);
            level.setBlock(WHEEL.below(), Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(WHEEL, SimBlocks.STEERING_WHEEL.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH), 3);
            level.setBlock(ASSEMBLER.below(2), Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(ASSEMBLER.below(), Blocks.OAK_PLANKS.defaultBlockState(), 3);
            level.setBlock(ASSEMBLER.below().south(), Blocks.OAK_PLANKS.defaultBlockState(), 3);
            level.setBlock(ASSEMBLER, SimBlocks.PHYSICS_ASSEMBLER.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR).setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH), 3);
            level.addFreshEntity(new SuperGlueEntity(level, new AABB(18, 120, 16, 19, 122, 18)));
            var player = s.getPlayerList().getPlayers().getFirst();
            check(player.isWithinBlockInteractionRange(WHEEL, 4) && player.isWithinBlockInteractionRange(ASSEMBLER, 4), "Real packet fixture is outside native player reach");
            return new int[]{player.getStats().getValue(Stats.CUSTOM, SimStats.INTERACT_WITH_STEERING_WHEEL.identifier().get()), player.getStats().getValue(Stats.CUSTOM, SimStats.INTERACT_WITH_ASSEMBLER.identifier().get())};
        });
        test.waitFor(client -> client.level.getBlockEntity(WHEEL) instanceof SteeringWheelBlockEntity, 200);
        test.runOnClient(client -> ClientPlayNetworking.send(new SteeringWheelPacket(false, 45, WHEEL)));
        server.waitFor(s -> {
            var player = s.getPlayerList().getPlayers().getFirst();
            return player.getStats().getValue(Stats.CUSTOM, SimStats.INTERACT_WITH_STEERING_WHEEL.identifier().get()) == before[0] + 1;
        }, 100);
        server.runOnServer(s -> {
            var wheel = (SteeringWheelBlockEntity) s.overworld().getBlockEntity(WHEEL);
            check(wheel.held && Math.abs(wheel.targetAngleToUpdate - 45) < .01, "Actual steering packet did not set held/target state");
        });
        test.runOnClient(client -> ClientPlayNetworking.send(new SteeringWheelPacket(true, 45, WHEEL)));
        server.waitFor(s -> !((SteeringWheelBlockEntity) s.overworld().getBlockEntity(WHEEL)).held, 100);
        test.runOnClient(client -> ClientPlayNetworking.send(new AssemblePacket(ASSEMBLER)));
        server.waitFor(s -> s.getPlayerList().getPlayers().getFirst().getStats().getValue(Stats.CUSTOM, SimStats.INTERACT_WITH_ASSEMBLER.identifier().get()) == before[1] + 1, 200);
        server.runOnServer(s -> {
            check(s.overworld().getBlockState(ASSEMBLER).isAir(), "Actual assembly packet did not transfer its controller");
            check(SubLevelContainer.getContainer(s.overworld()).getAllSubLevels().stream().anyMatch(v -> v instanceof ServerSubLevel && !v.isRemoved()), "Actual assembly packet did not create a native sublevel");
        });
        result.put("registeredStats", registered);
        result.put("packets", "actual Fabric client-to-server steering start/stop and assembly; each real award increased exactly once");
        checks.add("Reported stats NPE regression: all seven custom statistics/formatters registered; real Fabric steering start/stop and assembly packets handled, native server stats incremented exactly once and real plot created");
        var statsProof = new LinkedHashMap<>(result); statsProof.put("status", "passed");
        Files.writeString(REPORT.resolveSibling("stats-packets.json"), new GsonBuilder().setPrettyPrinting().create().toJson(statsProof));
        }

        test.runOnClient(client -> {
            var blocks = new LinkedHashMap<String, Object>();
            for (var id : BuiltInRegistries.BLOCK.keySet()) {
                var props = new LinkedHashMap<String, Object>();
                for (var p : BuiltInRegistries.BLOCK.getValue(id).getStateDefinition().getProperties()) props.put(p.getName(), propertyValues(p));
                blocks.put(id.toString(), props);
            }
            Files.writeString(REPORT.resolveSibling("block-registry.json"), new GsonBuilder().setPrettyPrinting().create().toJson(blocks));
            var loaded = new ArrayList<String>();
            for (var id : client.getResourceManager().listResources("ponder", p -> p.getPath().endsWith(".nbt") && Set.of("simulated", "aeronautics", "offroad").contains(p.getNamespace())).keySet()) {
                // The already-passed unchanged altitude scene is not repeated.
                if (id.toString().equals("simulated:ponder/sensor/altitude_sensor.nbt")) continue;
                var path = id.getPath(); var scene = id.withPath(path.substring("ponder/".length(), path.length() - ".nbt".length()));
                PonderSceneRegistry.loadSchematic(scene); loaded.add(id.toString());
            }
            result.put("nativePonderTemplatesLoaded", loaded);
        });
        var ponder = test.computeOnClient(client -> PonderUI.of(OffroadBlocks.BOREHEAD_BEARING_BLOCK.asItem().getDefaultInstance()));
        test.setScreen(() -> ponder);
        test.runOnClient(client -> {
            // Same public screen navigation as scrolling to the second story.
            check(ponder.mouseScrolled(10, 10, 0, 1), "Could not navigate to the actual excavating scene");
            check(ponder.getActiveScene().getId().toString().contains("excavating"), "Ponder selected a different scene: " + ponder.getActiveScene().getId());
        });
        var frames = new AtomicInteger();
        test.runOnClient(client -> ScreenEvents.afterExtract(ponder).register((screen, graphics, x, y, partial) -> frames.incrementAndGet()));
        test.waitFor(client -> ponder.getActiveScene().getCurrentTime() >= 35 && frames.get() >= 3, 500);
        var screenshot = test.takeScreenshot("offroad-excavating-crash-regression");
        result.put("ponderScene", test.computeOnClient(client -> ponder.getActiveScene().getId().toString()));
        result.put("ponderExtractedFrames", frames.get()); result.put("ponderScreenshot", screenshot.toString());
        test.runOnClient(client -> ponder.onClose());
        checks.add("Reported fatal Offroad Ponder regression: native template palette validation and actual excavating scene compiled, advanced to tick35 and extracted at least three frames; screenshot=" + screenshot);
        result.put("status", "passed");
        Files.writeString(REPORT, new GsonBuilder().setPrettyPrinting().create().toJson(result));
    }
    private static <T extends Comparable<T>> List<String> propertyValues(Property<T> property) { return property.getPossibleValues().stream().map(property::getName).toList(); }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
