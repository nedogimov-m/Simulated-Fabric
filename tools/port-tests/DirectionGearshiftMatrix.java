import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import dev.simulated_team.simulated.content.blocks.directional_gearshift.DirectionalGearshiftBlock;
import dev.simulated_team.simulated.content.blocks.directional_gearshift.DirectionalGearshiftBlockEntity;
import dev.simulated_team.simulated.index.SimBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Real motor/shaft/redstone matrix; persistent cells are reopened by a separate server process. */
public final class DirectionGearshiftMatrix {
    private static final String GROUP = "gearshift";
    private static final int BATCH = 24, SETTLE = 10;
    private static final int[] RPMS = {64, 0, -64};
    private static final List<Cell> CELLS = cells();
    private static int batch, stage, switchStep, liveEdge;
    private static long changedAt;
    private static String active = "setup";
    private static boolean switching, complete;

    private record Cell(int index, Direction facing, boolean first, Direction source, int power, int rpm) {
        BlockPos pos() { return DirectionMatrixSupport.base(GROUP, index); }
        String id() { return facing.getSerializedName() + "/axis-first-" + first + "/source-"
                + source.getSerializedName() + "/power-" + power + "/rpm-" + rpm; }
        Cell with(Direction face, int signal, int speed) { return new Cell(index, facing, first, face, signal, speed); }
        BlockState state() { return SimBlocks.DIRECTIONAL_GEARSHIFT.defaultBlockState()
                .setValue(DirectionalAxisKineticBlock.FACING, facing)
                .setValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE, first); }
        float output() { return power == 1 ? rpm : power == 2 ? -rpm : 0; }
    }

    private static List<Cell> cells() {
        List<Cell> cells = new ArrayList<>();
        for (Direction facing : Direction.values()) for (boolean first : new boolean[]{false, true}) {
            BlockState state = SimBlocks.DIRECTIONAL_GEARSHIFT.defaultBlockState()
                    .setValue(DirectionalAxisKineticBlock.FACING, facing)
                    .setValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE, first);
            Direction.Axis axis = SimBlocks.DIRECTIONAL_GEARSHIFT.getRotationAxis(state);
            for (Direction.AxisDirection side : Direction.AxisDirection.values()) {
                Direction source = Direction.get(side, axis);
                for (int power = 0; power < 4; power++) for (int rpm : RPMS)
                    cells.add(new Cell(cells.size(), facing, first, source, power, rpm));
            }
        }
        return List.copyOf(cells);
    }

    public static boolean tick(MinecraftServer server, List<String> checks) {
        if (complete) return true;
        try {
            ServerLevel level = DirectionMatrixSupport.level(server);
            if ("restore".equals(DirectionMatrixSupport.phase())) return restore(level, checks);
            if (switching) return sourceSwitch(level, checks);
            if (batch * BATCH >= CELLS.size()) { switching = true; batch = stage = 0; return false; }
            List<Cell> current = batch();
            if (stage == 0) {
                for (Cell cell : current) if (!done("cold/", cell)) { active = "cold/" + cell.id(); build(level, cell); }
                changedAt = level.getGameTime(); stage = 1; return false;
            }
            if (level.getGameTime() - changedAt < (stage == 1 ? 2 : SETTLE)) return false;
            if (stage == 1) {
                for (Cell cell : current) if (!done("cold/", cell)) motor(level, cell);
                changedAt = level.getGameTime(); stage = 2; return false;
            }
            if (stage == 2) {
                for (Cell cell : current) if (!done("cold/", cell)) verify(level, cell, "cold/", checks);
                prepareLive(level, current);
                changedAt = level.getGameTime(); stage = 3; return false;
            }
            if (stage == 3) {
                for (Cell cell : current) if (!done(livePrefix(cell), cell)) { power(level, cell); motor(level, cell); }
                changedAt = level.getGameTime(); stage = 4; return false;
            }
            for (Cell cell : current) if (!done(livePrefix(cell), cell)) verify(level, cell, livePrefix(cell), checks);
            if (++liveEdge < 3) {
                prepareLive(level, current);
                changedAt = level.getGameTime(); stage = 3; return false;
            }
            batch++; stage = liveEdge = 0; return false;
        } catch (Throwable failure) {
            DirectionMatrixSupport.fail(GROUP, active, failure);
            throw failure instanceof RuntimeException exception ? exception : new RuntimeException(failure);
        }
    }

    private static boolean restore(ServerLevel level, List<String> checks) {
        if (batch * BATCH >= CELLS.size()) { complete = true; return true; }
        if (stage == 0) {
            for (Cell cell : batch()) if (!done("restore/", cell)) load(level, cell.pos());
            changedAt = level.getGameTime(); stage = 1; return false;
        }
        if (level.getGameTime() - changedAt < SETTLE) return false;
        for (Cell cell : batch()) if (!done("restore/", cell)) verify(level, cell, "restore/", checks);
        batch++; stage = 0; return false;
    }

    private static boolean sourceSwitch(ServerLevel level, List<String> checks) {
        // One left-only/+64 cell per orientation/source group. Every switch state has its own durable ID.
        List<Cell> current = new ArrayList<>();
        for (int group = 0; group < 24; group++) current.add(CELLS.get(group * 12 + 3));
        if (switchStep >= 12) {
            if (stage == 0) {
                for (Cell cell : current) { active = "snapshot/" + cell.id(); build(level, cell); }
                changedAt = level.getGameTime(); stage = 1; return false;
            }
            if (level.getGameTime() - changedAt < (stage == 1 ? 2 : SETTLE)) return false;
            if (stage == 1) {
                for (Cell cell : current) motor(level, cell);
                changedAt = level.getGameTime(); stage = 2; return false;
            }
            for (Cell cell : current) if (!done("snapshot/", cell)) verify(level, cell, "snapshot/", checks);
            complete = true; return true;
        }
        int power = switchStep / 3, rpm = RPMS[switchStep % 3];
        if (stage == 0) {
            for (Cell original : current) {
                Cell target = original.with(original.source.getOpposite(), power, rpm);
                if (done("switch/", target)) continue;
                active = "switch/" + target.id();
                if (switchStep == 0) switchSource(level, original, target); else power(level, target);
            }
            changedAt = level.getGameTime(); stage = 1; return false;
        }
        if (level.getGameTime() - changedAt < (stage == 1 ? 2 : SETTLE)) return false;
        if (stage == 1) {
            for (Cell original : current) {
                Cell target = original.with(original.source.getOpposite(), power, rpm);
                if (!done("switch/", target)) motor(level, target);
            }
            changedAt = level.getGameTime(); stage = 2; return false;
        }
        for (Cell original : current) {
            Cell target = original.with(original.source.getOpposite(), power, rpm);
            if (!done("switch/", target)) verify(level, target, "switch/", checks);
        }
        switchStep++; stage = 0; return false;
    }

    private static List<Cell> batch() { return CELLS.subList(batch * BATCH, Math.min((batch + 1) * BATCH, CELLS.size())); }
    private static String livePrefix(Cell cell) { return "live/from-power-" + ((cell.power + liveEdge + 1) % 4) + "/"; }
    private static void prepareLive(ServerLevel level, List<Cell> current) {
        for (Cell cell : current) if (!done(livePrefix(cell), cell)) {
            active = livePrefix(cell) + cell.id();
            // Every directed redstone edge, driven by a real reversed/stopped motor before the target state.
            Cell previous = cell.with(cell.source, (cell.power + liveEdge + 1) % 4, cell.rpm == 0 ? 64 : -cell.rpm);
            power(level, previous); motor(level, previous);
        }
    }
    private static boolean done(String prefix, Cell cell) { return DirectionMatrixSupport.done(GROUP, prefix + cell.id()); }
    private static void load(ServerLevel level, BlockPos pos) {
        level.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
        level.getChunk(pos);
    }

    private static void build(ServerLevel level, Cell cell) {
        BlockPos pos = cell.pos(); load(level, pos);
        for (Direction face : Direction.values()) level.setBlockAndUpdate(pos.relative(face), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        power(level, cell);
        BlockState state = SimBlocks.DIRECTIONAL_GEARSHIFT.getPoweredState(level, cell.state(), pos);
        level.setBlockAndUpdate(pos, state);
        Direction.Axis axis = SimBlocks.DIRECTIONAL_GEARSHIFT.getRotationAxis(state);
        level.setBlockAndUpdate(pos.relative(cell.source.getOpposite()), AllBlocks.SHAFT.defaultBlockState()
                .setValue(BlockStateProperties.AXIS, axis));
        level.setBlockAndUpdate(pos.relative(cell.source), AllBlocks.CREATIVE_MOTOR.defaultBlockState()
                .setValue(CreativeMotorBlock.FACING, cell.source.getOpposite()));
    }

    private static void power(ServerLevel level, Cell cell) {
        Direction left = SimBlocks.DIRECTIONAL_GEARSHIFT.getLeftDirection(cell.state());
        Direction right = SimBlocks.DIRECTIONAL_GEARSHIFT.getRightDirection(cell.state());
        level.setBlockAndUpdate(cell.pos().relative(left), (cell.power % 2 != 0 ? Blocks.REDSTONE_BLOCK : Blocks.AIR).defaultBlockState());
        level.setBlockAndUpdate(cell.pos().relative(right), (cell.power >= 2 ? Blocks.REDSTONE_BLOCK : Blocks.AIR).defaultBlockState());
    }

    private static void switchSource(ServerLevel level, Cell original, Cell target) {
        BlockPos pos = original.pos();
        DirectionMatrixSupport.check(level.getBlockEntity(pos) instanceof DirectionalGearshiftBlockEntity,
                "source switch requires the existing genuine gearshift owner");
        Object owner = level.getBlockEntity(pos);
        // Remove the former source and move the real motor to the opposite shaft end without replacing the gearshift.
        level.setBlockAndUpdate(pos.relative(original.source), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.relative(target.source), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.relative(original.source), AllBlocks.SHAFT.defaultBlockState()
                .setValue(BlockStateProperties.AXIS, SimBlocks.DIRECTIONAL_GEARSHIFT.getRotationAxis(original.state())));
        level.setBlockAndUpdate(pos.relative(target.source), AllBlocks.CREATIVE_MOTOR.defaultBlockState()
                .setValue(CreativeMotorBlock.FACING, target.source.getOpposite()));
        power(level, target);
        DirectionMatrixSupport.check(level.getBlockEntity(pos) == owner, "source switch replaced gearshift owner");
    }

    private static void motor(ServerLevel level, Cell cell) {
        BlockPos pos = cell.pos().relative(cell.source);
        DirectionMatrixSupport.check(level.getBlockEntity(pos) instanceof CreativeMotorBlockEntity,
                cell.id() + ": real motor missing at " + pos);
        CreativeMotorBlockEntity motor = (CreativeMotorBlockEntity) level.getBlockEntity(pos);
        Direction facing = cell.source.getOpposite();
        motor.generatedSpeed.setValue(cell.rpm * facing.getAxisDirection().getStep());
    }

    private static void verify(ServerLevel level, Cell cell, String prefix, List<String> checks) {
        active = prefix + cell.id();
        BlockPos pos = cell.pos(), source = pos.relative(cell.source), output = pos.relative(cell.source.getOpposite());
        DirectionMatrixSupport.check(level.getBlockState(pos).is(SimBlocks.DIRECTIONAL_GEARSHIFT), active + ": gearshift destroyed");
        DirectionMatrixSupport.check(level.getBlockState(source).is(AllBlocks.CREATIVE_MOTOR), active + ": motor destroyed");
        DirectionMatrixSupport.check(level.getBlockState(output).is(AllBlocks.SHAFT), active + ": output shaft destroyed");
        BlockState state = level.getBlockState(pos);
        DirectionMatrixSupport.check(state.getValue(DirectionalAxisKineticBlock.FACING) == cell.facing
                && state.getValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE) == cell.first,
                active + ": orientation changed");
        DirectionMatrixSupport.check(state.getValue(DirectionalGearshiftBlock.LEFT_POWERED) == (cell.power % 2 != 0)
                && state.getValue(DirectionalGearshiftBlock.RIGHT_POWERED) == (cell.power >= 2), active + ": actual redstone state mismatch");
        DirectionMatrixSupport.check(level.getBlockEntity(pos) instanceof DirectionalGearshiftBlockEntity, active + ": native gearshift owner missing");
        KineticBlockEntity gearshift = (KineticBlockEntity) level.getBlockEntity(pos);
        KineticBlockEntity shaft = (KineticBlockEntity) level.getBlockEntity(output);
        KineticBlockEntity motor = (KineticBlockEntity) level.getBlockEntity(source);
        near(motor.getSpeed(), cell.rpm, active + ": motor RPM");
        near(gearshift.getPortSpeed(cell.source), cell.rpm, active + ": input port RPM");
        near(gearshift.getPortSpeed(cell.source.getOpposite()), cell.output(), active + ": output port RPM");
        near(shaft.getSpeed(), cell.output(), active + ": actual output owner RPM");
        near(shaft.getTheoreticalSpeed(), cell.output(), active + ": output theoretical RPM");
        DirectionMatrixSupport.check(!gearshift.isConflictDestructionPending() && !motor.isConflictDestructionPending()
                && !shaft.isConflictDestructionPending(), active + ": native kinetic conflict pending");
        if (cell.rpm != 0) {
            DirectionMatrixSupport.check(source.equals(gearshift.getSource()) && source.equals(gearshift.getGeneratingSource())
                    && gearshift.getKineticSourceFace() == cell.source, active + ": input provenance mismatch");
        }
        if (cell.output() != 0) {
            DirectionMatrixSupport.check(pos.equals(shaft.getSource()) && source.equals(shaft.getGeneratingSource())
                    && shaft.getKineticSourceFace() == cell.source, active + ": output provenance mismatch");
        }
        Map<String,Object> evidence = new LinkedHashMap<>();
        evidence.put("fixture", List.of(pos.getX(),pos.getY(),pos.getZ()));
        evidence.put("facing",cell.facing.getSerializedName()); evidence.put("axisAlongFirst",cell.first);
        evidence.put("sourceFace",cell.source.getSerializedName()); evidence.put("power",cell.power);
        evidence.put("motorRpm",motor.getSpeed()); evidence.put("inputPortRpm",gearshift.getPortSpeed(cell.source));
        evidence.put("outputPortRpm",gearshift.getPortSpeed(cell.source.getOpposite())); evidence.put("outputOwnerRpm",shaft.getSpeed());
        evidence.put("expectedOutput",cell.output()); evidence.put("nativeSaveRestore",prefix.equals("restore/"));
        DirectionMatrixSupport.pass(GROUP, active, evidence);
        checks.add(GROUP + "/" + active);
    }

    private static void near(float actual, float expected, String message) {
        DirectionMatrixSupport.check(Float.isFinite(actual) && Math.abs(actual - expected) < 0.01f,
                message + ": expected " + expected + ", got " + actual);
    }
    private DirectionGearshiftMatrix() {}
}
