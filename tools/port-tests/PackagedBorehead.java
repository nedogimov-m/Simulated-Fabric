import com.google.gson.GsonBuilder;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.ryanhcode.offroad.content.blocks.borehead_bearing.BoreheadBearingBlockEntity;
import dev.ryanhcode.offroad.content.contraptions.borehead_contraption.BoreheadBearingContraption;
import dev.ryanhcode.offroad.content.entities.BoreheadContraptionEntity;
import dev.ryanhcode.offroad.index.OffroadBlocks;
import dev.simulated_team.simulated.content.blocks.util.AbstractDirectionalAxisBlock;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

/** Only the reported real-world borehead bearing ticking/assembly boundary. */
public final class PackagedBorehead {
    private static final BlockPos BEARING = new BlockPos(0, 120, 0), MOTOR = BEARING.west(),
            CHEST = BEARING.north(), WHEEL = CHEST.east();

    public static void run(ClientGameTestContext test, TestSingleplayerContext world, List<String> checks) throws Exception {
        var report = new LinkedHashMap<String, Object>();
        var completed = new ArrayList<String>(); report.put("completed", completed);
        var path = Path.of(System.getProperty("port.smoke.report")).resolveSibling("borehead.json");
        var server = world.getServer();
        try {
            server.runCommand("gamemode creative @a"); server.runCommand("time set noon");
            server.runCommand("tp @a 4 123 -6 35 23");
            server.runOnServer(s -> {
                var level = s.overworld(); level.getChunk(BEARING); level.getChunk(MOTOR); level.getChunk(CHEST);
                check(level.setBlock(BEARING, OffroadBlocks.BOREHEAD_BEARING_BLOCK.defaultBlockState()
                        .setValue(BlockStateProperties.FACING, Direction.NORTH)
                        .setValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE, true), 3), "Could not place the genuine borehead bearing");
            });
            test.waitFor(client -> client.level.getBlockEntity(BEARING) instanceof BoreheadBearingBlockEntity && client.gui.overlay() == null, 600);
            // Idle ticking already passed in the preserved first attempt. This
            // resumed case starts at the newly changed powered port boundary.
            write(path, report);
            server.runOnServer(s -> {
                var level = s.overworld();
                level.setBlock(CHEST, Blocks.CHEST.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH), 3);
                ((ChestBlockEntity) level.getBlockEntity(CHEST)).setItem(0, new ItemStack(Items.COBBLESTONE, 7));
                level.setBlock(WHEEL, OffroadBlocks.ROCK_CUTTER_BLOCK.defaultBlockState()
                        .setValue(BlockStateProperties.FACING, Direction.NORTH)
                        .setValue(AbstractDirectionalAxisBlock.AXIS_ALONG_FIRST_COORDINATE, true), 3);
                level.addFreshEntity(new SuperGlueEntity(level, new AABB(0, 120, -1, 2, 121, 0)));
                level.setBlock(MOTOR, AllBlocks.CREATIVE_MOTOR.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.EAST), 3);
                ((CreativeMotorBlockEntity) level.getBlockEntity(MOTOR)).generatedSpeed.setValue(64);
                ((BoreheadBearingBlockEntity) level.getBlockEntity(BEARING)).setAssembleNextTick(true);
            });
            server.waitFor(s -> {
                var bearing = (BoreheadBearingBlockEntity) s.overworld().getBlockEntity(BEARING);
                return bearing.isRunning() && bearing.getMovedContraption() != null;
            }, 200);
            UUID rotor = server.computeOnServer(s -> {
                var level = s.overworld(); var bearing = (BoreheadBearingBlockEntity) level.getBlockEntity(BEARING);
                var entity = bearing.getMovedContraption();
                check(bearing.getSpeed() == -64, "West source did not preserve borehead negative port polarity: " + bearing.getSpeed());
                check(entity.getContraption() instanceof BoreheadBearingContraption, "Actual bearing did not use its addon contraption factory");
                check(entity.getContraption().rockCuttingWheelAmount == 1, "Actual assembled rotor lost its single rock cutting wheel");
                check(entity.getContraption().getBlocks().size() == 2, "Unexpected selected borehead rotor blocks");
                check(!entity.getContraption().getStorage().getAllItemStorages().isEmpty() && bearing.getContraptionWrappedInventory() != null, "Actual chest was not mounted on the borehead rotor");
                check(level.getBlockState(CHEST).isAir() && level.getBlockState(WHEEL).isAir(), "Native assembly did not transfer the rotor blocks");
                report.put("rotorUuid", entity.getUUID().toString()); report.put("motorRpm", bearing.getSpeed());
                return entity.getUUID();
            });
            test.waitFor(client -> client.level.getBlockEntity(BEARING) instanceof BoreheadBearingBlockEntity bearing
                    && bearing.getMovedContraption() != null && bearing.getMovedContraption().getUUID().equals(rotor)
                    && bearing.getMovedContraption().getContraption() instanceof BoreheadBearingContraption, 300);
            completed.add("actual Create motor network assembled the genuine Offroad entity/contraption with one cutting wheel and a real mounted chest; client resolved the same UUID");
            write(path, report);
            float serverAngle = server.computeOnServer(s -> ((BoreheadBearingBlockEntity) s.overworld().getBlockEntity(BEARING)).getInterpolatedAngle(1));
            float clientAngle = test.computeOnClient(c -> ((BoreheadBearingBlockEntity) c.level.getBlockEntity(BEARING)).getInterpolatedAngle(1));
            test.waitTicks(100);
            server.runOnServer(s -> {
                var bearing = (BoreheadBearingBlockEntity) s.overworld().getBlockEntity(BEARING);
                check(bearing.getMovedContraption() != null && bearing.getMovedContraption().getUUID().equals(rotor), "Real running rotor owner was lost");
                float angle = bearing.getInterpolatedAngle(1);
                check(Float.isFinite(angle) && Math.abs(angle - serverAngle) > 3 && Math.abs(bearing.getRotationSpeed()) > .01, "Actual borehead did not rotate: " + angle + " from " + serverAngle);
                report.put("serverAngleBefore", serverAngle); report.put("serverAngleAfter", angle); report.put("rotationSpeed", bearing.getRotationSpeed());
            });
            test.runOnClient(client -> {
                var bearing = (BoreheadBearingBlockEntity) client.level.getBlockEntity(BEARING);
                float angle = bearing.getInterpolatedAngle(1);
                check(bearing.getMovedContraption() != null && Float.isFinite(angle) && Math.abs(angle - clientAngle) > 3, "Actual client borehead did not receive/render rotation");
                report.put("clientAngleBefore", clientAngle); report.put("clientAngleAfter", angle);
            });
            report.put("screenshot", test.takeScreenshot("borehead-real-rotation").toString());
            completed.add("real bearing and addon rotor survived 100 powered ticks with changing finite server/client angle and same native owner");
            write(path, report);
            server.runOnServer(s -> {
                var level = s.overworld();
                ((CreativeMotorBlockEntity) level.getBlockEntity(MOTOR)).generatedSpeed.setValue(0);
                var bearing = (BoreheadBearingBlockEntity) level.getBlockEntity(BEARING);
                check(bearing.disassemble(), "Native borehead disassembly refused");
                check(!bearing.isRunning() && bearing.getMovedContraption() == null, "Disassembled bearing retained its native rotor owner");
                check(level.getBlockState(CHEST).is(Blocks.CHEST) && level.getBlockState(WHEEL).is(OffroadBlocks.ROCK_CUTTER_BLOCK), "Native disassembly did not restore exact rotor positions");
                var chest = (ChestBlockEntity) level.getBlockEntity(CHEST);
                check(chest.getItem(0).is(Items.COBBLESTONE) && chest.getItem(0).getCount() == 7, "Actual mounted chest contents changed during disassembly");
            });
            test.waitFor(client -> client.level.getBlockEntity(BEARING) instanceof BoreheadBearingBlockEntity bearing
                    && !bearing.isRunning() && bearing.getMovedContraption() == null, 200);
            test.waitTicks(20);
            completed.add("native disassembly retired the rotor on both sides, restored chest/wheel positions and all seven stored cobblestone; 20 idle ticks after stop");
            // Reverse the actual source side without assembling another rotor:
            // the same static port ratios must remain consistent through a live
            // shaft on the opposite face, including stop and sign reversal.
            BlockPos east = BEARING.east();
            server.runOnServer(s -> {
                var level = s.overworld();
                level.setBlock(MOTOR, AllBlocks.SHAFT.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X), 3);
                level.setBlock(east, AllBlocks.CREATIVE_MOTOR.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.WEST), 3);
                ((CreativeMotorBlockEntity) level.getBlockEntity(east)).generatedSpeed.setValue(-64);
            });
            for (int dial : new int[]{-64, 0, 64}) {
                server.runOnServer(s -> ((CreativeMotorBlockEntity) s.overworld().getBlockEntity(east)).generatedSpeed.setValue(dial));
                test.waitTicks(35);
                server.runOnServer(s -> {
                    var bearing = (BoreheadBearingBlockEntity) s.overworld().getBlockEntity(BEARING);
                    var shaft = (KineticBlockEntity) s.overworld().getBlockEntity(MOTOR);
                    check(bearing != null && shaft != null, "Opposite-side source destroyed the bearing or passive shaft");
                    check(bearing.getSpeed() == -dial && shaft.getSpeed() == dial,
                            "Canonical graph and borehead opposite port signs disagree: dial=" + dial + ", bearing=" + bearing.getSpeed() + ", shaft=" + shaft.getSpeed());
                });
            }
            server.runOnServer(s -> ((CreativeMotorBlockEntity) s.overworld().getBlockEntity(east)).generatedSpeed.setValue(0));
            completed.add("actual east-side motor and west passive shaft preserved opposite borehead port signs at +64, zero and -64 RPM, with 35 stable ticks at each setting");
            checks.addAll(completed); report.put("status", "passed");
        } catch (Throwable failure) {
            report.put("status", "failed"); report.put("error", failure.toString()); throw failure;
        } finally { write(path, report); }
    }
    private static void write(Path path, Object report) throws java.io.IOException { Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(report)); }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
