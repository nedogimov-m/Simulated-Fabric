import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import dev.ryanhcode.offroad.content.blocks.borehead_bearing.BoreheadBearingBlockEntity;
import dev.ryanhcode.offroad.index.OffroadBlocks;
import dev.simulated_team.simulated.content.blocks.util.AbstractDirectionalAxisBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Live registered motor/borehead/shaft graphs, native rotor assembly and full-process world reload. */
public final class DirectionBoreheadMatrix {
    private static final String GROUP = "borehead";
    private static final int BATCH = 8, SETTLE = 10, STABLE = 4, TIMEOUT = 180;
    private static final boolean RESTORE = "restore".equals(DirectionMatrixSupport.phase());
    private static final List<Cell> CELLS = cells();
    private static final Set<Long> FORCED = new LinkedHashSet<>();
    private static int batch;
    private static String active = "setup";
    private static boolean complete;

    private record Orientation(Direction facing, boolean first, Direction.Axis axis) {
        String id() { return facing.getSerializedName() + "-first-" + first + "-shaft-" + axis.getSerializedName(); }
        BlockState state() { return OffroadBlocks.BOREHEAD_BEARING_BLOCK.defaultBlockState()
                .setValue(DirectionalAxisKineticBlock.FACING, facing)
                .setValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE, first); }
    }
    private record Step(String id, int dial, boolean switched, boolean absent) {}
    private static final class Cell {
        final int index;
        final Orientation first, second;
        final Direction source;
        final boolean rotor;
        final BlockPos center;
        final List<Step> steps;
        int step, consecutive;
        long startedAt;
        boolean initialized, preparing, finished;
        String lastFailure = "no stable observation yet";
        Object owner, secondOwner;
        UUID rotorUuid;
        float angleBefore;
        Cell(int index, Orientation first, Orientation second, Direction source, boolean rotor) {
            this.index = index; this.first = first; this.second = second; this.source = source; this.rotor = rotor;
            center = DirectionMatrixSupport.base(GROUP, index); steps = stages(this);
        }
        String id() { return (rotor ? "rotor/" : second == null ? "single/" : "chain/") + first.id()
                + (second == null ? "" : "/then-" + second.id()) + "/source-" + source.getSerializedName(); }
        String key() { return id() + "/" + steps.get(step).id; }
        BlockPos near() { return center.relative(source); }
        BlockPos secondPos() { return center.relative(source.getOpposite()); }
        BlockPos far() { return center.relative(source.getOpposite(), second == null ? 1 : 2); }
        BlockPos chest() { return center.relative(first.facing); }
        BlockPos cutter() { return center.relative(first.facing, 2); }
    }

    private static List<Orientation> orientations() {
        List<Orientation> result = new ArrayList<>();
        for (Direction facing : Direction.values()) for (boolean first : new boolean[]{false, true}) {
            Direction.Axis axis = switch (facing.getAxis()) {
                case X -> first ? Direction.Axis.Y : Direction.Axis.Z;
                case Y -> first ? Direction.Axis.X : Direction.Axis.Z;
                case Z -> first ? Direction.Axis.X : Direction.Axis.Y;
            };
            result.add(new Orientation(facing, first, axis));
        }
        return result;
    }
    private static List<Cell> cells() {
        List<Cell> result = new ArrayList<>();
        List<Orientation> orientations = orientations();
        for (Orientation o : orientations) for (Direction.AxisDirection sign : Direction.AxisDirection.values())
            result.add(new Cell(result.size(), o, null, Direction.get(sign, o.axis), false));
        for (Orientation a : orientations) for (Orientation b : orientations) if (a.axis == b.axis)
            for (Direction.AxisDirection sign : Direction.AxisDirection.values())
                result.add(new Cell(result.size(), a, b, Direction.get(sign, a.axis), false));
        if (!RESTORE) for (Orientation o : orientations) if (!(o.facing == Direction.NORTH && o.first))
            result.add(new Cell(result.size(), o, null, Direction.get(Direction.AxisDirection.NEGATIVE, o.axis), true));
        return List.copyOf(result);
    }
    private static List<Step> stages(Cell cell) {
        if (RESTORE) return List.of(new Step("cold-world-load", 64, true, false),
                new Step("reverse-after-load", -64, true, false), new Step("zero-after-load", 0, true, false),
                new Step("resume-after-load", 64, true, false));
        if (cell.rotor) return List.of(new Step("assemble", 64, false, false),
                new Step("rotate", 64, false, false), new Step("disassemble", 0, false, false));
        List<Step> result = new ArrayList<>();
        result.add(new Step("cold-source-placement", 64, false, false));
        // NORTH/first=true warm +/-/zero and real rotor were already accepted by PackagedBorehead.
        if (cell.second != null || cell.first.facing != Direction.NORTH || !cell.first.first) {
            result.add(new Step("reverse", -64, false, false));
            result.add(new Step("zero", 0, false, false));
            result.add(new Step("resume", 64, false, false));
        }
        if (cell.second == null) {
            result.add(new Step("source-removed", 0, false, true));
            result.add(new Step("cold-source-replacement", 64, false, false));
        }
        result.add(new Step("source-switch", 64, true, false));
        result.add(new Step("reverse-switched", -64, true, false));
        result.add(new Step("zero-switched", 0, true, false));
        result.add(new Step("persist-final", 64, true, false));
        return List.copyOf(result);
    }

    public static boolean tick(MinecraftServer server, List<String> checks) {
        if (complete) return true;
        try {
            ServerLevel level = DirectionMatrixSupport.level(server);
            if (batch * BATCH >= CELLS.size()) { release(level); complete = true; return true; }
            List<Cell> current = CELLS.subList(batch * BATCH, Math.min((batch + 1) * BATCH, CELLS.size()));
            for (Cell cell : current) {
                if (cell.finished) continue;
                if (!cell.initialized) initialize(level, cell);
                if (cell.finished) continue;
                active = cell.key();
                Step target = cell.steps.get(cell.preparing ? cell.step - 1 : cell.step);
                long elapsed = level.getGameTime() - cell.startedAt;
                if (elapsed < SETTLE) continue;
                try {
                    Map<String, Object> evidence = observe(level, cell, target, elapsed);
                    if (++cell.consecutive < STABLE) continue;
                    if (cell.preparing) {
                        cell.preparing = false; begin(level, cell, cell.steps.get(cell.step)); continue;
                    }
                    evidence.put("stableConsecutiveTicks", cell.consecutive);
                    evidence.put("phase", DirectionMatrixSupport.phase());
                    evidence.put("genuineProcessWorldReload", RESTORE);
                    DirectionMatrixSupport.pass(GROUP, cell.key(), evidence); checks.add(GROUP + "/" + cell.key());
                    do { cell.step++; } while (cell.step < cell.steps.size() && DirectionMatrixSupport.done(GROUP, cell.key()));
                    if (cell.step == cell.steps.size()) cell.finished = true;
                    else begin(level, cell, cell.steps.get(cell.step));
                } catch (AssertionError transientFailure) {
                    cell.consecutive = 0; cell.lastFailure = transientFailure.toString();
                    if (elapsed >= TIMEOUT) throw new AssertionError(active + ": no stable native state after " + elapsed
                            + " actual ticks; " + cell.lastFailure, transientFailure);
                }
            }
            if (current.stream().allMatch(cell -> cell.finished)) { release(level); batch++; }
            return false;
        } catch (Throwable failure) {
            DirectionMatrixSupport.fail(GROUP, active, failure);
            throw failure instanceof RuntimeException exception ? exception : new RuntimeException(failure);
        }
    }

    private static void initialize(ServerLevel level, Cell cell) {
        while (cell.step < cell.steps.size() && DirectionMatrixSupport.done(GROUP, cell.key())) cell.step++;
        cell.initialized = true;
        if (cell.step == cell.steps.size()) { cell.finished = true; return; }
        active = cell.key(); load(level, cell);
        if (!RESTORE) build(level, cell);
        else {
            // This branch deliberately reads the genuinely loaded owners before any block or dial mutation.
            cell.owner = level.getBlockEntity(cell.center);
            cell.secondOwner = cell.second == null ? null : level.getBlockEntity(cell.secondPos());
        }
        cell.preparing = cell.step > 0;
        begin(level, cell, cell.steps.get(cell.preparing ? cell.step - 1 : cell.step));
    }
    private static void begin(ServerLevel level, Cell cell, Step step) {
        cell.startedAt = level.getGameTime(); cell.consecutive = 0;
        if (!(RESTORE && step.id.equals("cold-world-load") && !cell.preparing)) apply(level, cell, step);
        if (cell.rotor && step.id.equals("rotate")) {
            var bearing = bearing(level, cell.center);
            var entity = bearing.getMovedContraption();
            if (entity != null) { cell.rotorUuid = entity.getUUID(); cell.angleBefore = bearing.getInterpolatedAngle(1); }
        }
    }
    private static void load(ServerLevel level, Cell cell) {
        int minX = (cell.center.getX() - 3) >> 4, maxX = (cell.center.getX() + 3) >> 4;
        int minZ = (cell.center.getZ() - 3) >> 4, maxZ = (cell.center.getZ() + 3) >> 4;
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            long key = (long) x << 32 | z & 0xffffffffL;
            if (FORCED.add(key)) level.setChunkForced(x, z, true);
            level.getChunk(x, z);
        }
    }
    private static void release(ServerLevel level) {
        for (long chunk : FORCED) level.setChunkForced((int) (chunk >> 32), (int) chunk, false);
        FORCED.clear();
    }
    private static void build(ServerLevel level, Cell cell) {
        for (BlockPos pos : List.of(cell.center, cell.secondPos()))
            if (level.getBlockEntity(pos) instanceof BoreheadBearingBlockEntity existing && existing.isRunning())
                DirectionMatrixSupport.check(existing.disassemble(), "Could not retire previous real rotor at " + pos);
        for (BlockPos pos : BlockPos.betweenClosed(cell.center.offset(-3, -3, -3), cell.center.offset(3, 3, 3)))
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(cell.center, cell.first.state());
        if (cell.second != null) level.setBlockAndUpdate(cell.secondPos(), cell.second.state());
        level.setBlockAndUpdate(cell.far(), shaft(cell));
        if (cell.rotor) {
            level.setBlockAndUpdate(cell.chest(), Blocks.CHEST.defaultBlockState());
            ((ChestBlockEntity) level.getBlockEntity(cell.chest())).setItem(0, new ItemStack(Items.COBBLESTONE, 7));
            level.setBlockAndUpdate(cell.cutter(), OffroadBlocks.ROCK_CUTTER_BLOCK.defaultBlockState()
                    .setValue(BlockStateProperties.FACING, cell.first.facing)
                    .setValue(AbstractDirectionalAxisBlock.AXIS_ALONG_FIRST_COORDINATE, cell.first.first));
            level.addFreshEntity(new SuperGlueEntity(level, new AABB(cell.chest()).minmax(new AABB(cell.cutter()))));
        }
        level.setBlockAndUpdate(cell.near(), motorState(cell.source.getOpposite()));
        cell.owner = level.getBlockEntity(cell.center);
        cell.secondOwner = cell.second == null ? null : level.getBlockEntity(cell.secondPos());
    }
    private static BlockState shaft(Cell cell) {
        return AllBlocks.SHAFT.defaultBlockState().setValue(BlockStateProperties.AXIS, cell.first.axis);
    }
    private static BlockState motorState(Direction facing) {
        return AllBlocks.CREATIVE_MOTOR.defaultBlockState().setValue(CreativeMotorBlock.FACING, facing);
    }
    private static void apply(ServerLevel level, Cell cell, Step step) {
        BlockPos motorPos = step.switched ? cell.far() : cell.near();
        BlockPos outputPos = step.switched ? cell.near() : cell.far();
        if (step.absent) level.setBlockAndUpdate(motorPos, Blocks.AIR.defaultBlockState());
        else {
            if (!level.getBlockState(outputPos).is(AllBlocks.SHAFT)) {
                level.setBlockAndUpdate(outputPos, Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(motorPos, Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(outputPos, shaft(cell));
            }
            Direction facing = step.switched ? cell.source : cell.source.getOpposite();
            if (!level.getBlockState(motorPos).is(AllBlocks.CREATIVE_MOTOR))
                level.setBlockAndUpdate(motorPos, motorState(facing));
            DirectionMatrixSupport.check(level.getBlockEntity(motorPos) instanceof CreativeMotorBlockEntity,
                    cell.id() + ": native creative motor missing");
            ((CreativeMotorBlockEntity) level.getBlockEntity(motorPos)).generatedSpeed.setValue(step.dial);
        }
        DirectionMatrixSupport.check(level.getBlockEntity(cell.center) == cell.owner
                && (cell.second == null || level.getBlockEntity(cell.secondPos()) == cell.secondOwner),
                cell.id() + ": source transition replaced native borehead owner");
        if (cell.rotor) {
            var bearing = bearing(level, cell.center);
            if (step.id.equals("disassemble")) DirectionMatrixSupport.check(bearing.disassemble(), "Native borehead disassembly refused");
            else if (!bearing.isRunning()) bearing.setAssembleNextTick(true);
        }
    }

    private static Map<String, Object> observe(ServerLevel level, Cell cell, Step step, long elapsed) {
        String id = cell.id() + "/" + step.id;
        BlockPos motorPos = step.switched ? cell.far() : cell.near();
        BlockPos outputPos = step.switched ? cell.near() : cell.far();
        Direction inputFace = step.switched ? cell.source.getOpposite() : cell.source;
        int side = cell.source.getAxisDirection().getStep();
        float motorRpm = step.absent ? 0 : (step.switched ? side : -side) * step.dial;
        float outputRpm = cell.second == null ? -motorRpm : motorRpm;
        float firstRpm = step.absent ? 0 : cell.second != null && step.switched ? step.dial : -step.dial;
        float secondRpm = step.switched ? -step.dial : step.dial;
        checkOrientation(level, cell.center, cell.first); checkOwner(level, cell.center, cell.owner);
        var first = bearing(level, cell.center);
        var output = kinetic(level, outputPos);
        DirectionMatrixSupport.check(level.getBlockState(outputPos).is(AllBlocks.SHAFT), id + ": output shaft lost");
        DirectionMatrixSupport.check(level.getBlockState(outputPos).getValue(BlockStateProperties.AXIS) == cell.first.axis,
                id + ": shaft axis changed");
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("fixture", coordinates(cell.center)); evidence.put("facing", cell.first.facing.getSerializedName());
        evidence.put("axisAlongFirst", cell.first.first); evidence.put("shaftAxis", cell.first.axis.getSerializedName());
        evidence.put("initialSourceFace", cell.source.getSerializedName()); evidence.put("switched", step.switched);
        evidence.put("dial", step.dial); evidence.put("elapsedActualTicks", elapsed);
        evidence.put("expectedMotorRpm", motorRpm); evidence.put("expectedFirstBearingRpm", firstRpm);
        evidence.put("expectedOutputRpm", outputRpm);
        if (step.absent) DirectionMatrixSupport.check(level.getBlockState(motorPos).isAir(), id + ": old motor not removed");
        else {
            DirectionMatrixSupport.check(level.getBlockState(motorPos).is(AllBlocks.CREATIVE_MOTOR), id + ": motor lost");
            DirectionMatrixSupport.check(level.getBlockState(motorPos).getValue(CreativeMotorBlock.FACING) == inputFace.getOpposite(),
                    id + ": motor shaft face changed");
            var motor = (CreativeMotorBlockEntity) level.getBlockEntity(motorPos);
            DirectionMatrixSupport.check(motor.generatedSpeed.getValue() == step.dial, id + ": actual saved motor dial differs");
            speed(motor, motorRpm, id + ": motor"); evidence.put("motor", snapshot(motor));
        }
        speed(first, firstRpm, id + ": first bearing"); speed(output, outputRpm, id + ": output shaft");
        evidence.put("firstBearing", snapshot(first)); evidence.put("outputShaft", snapshot(output));
        if (cell.second != null) {
            checkOrientation(level, cell.secondPos(), cell.second); checkOwner(level, cell.secondPos(), cell.secondOwner);
            var second = bearing(level, cell.secondPos()); speed(second, secondRpm, id + ": second bearing");
            DirectionMatrixSupport.check(!second.isRunning() && second.getMovedContraption() == null, id + ": static second bearing unexpectedly assembled");
            evidence.put("secondOrientation", cell.second.id()); evidence.put("secondBearing", snapshot(second));
            evidence.put("expectedSecondBearingRpm", secondRpm);
            if (step.dial != 0 && !step.absent) provenance(second, step.switched ? motorPos : cell.center, motorPos, inputFace, id);
        }
        if (step.dial != 0 && !step.absent) {
            provenance(first, cell.second != null && step.switched ? cell.secondPos() : motorPos, motorPos, inputFace, id);
            provenance(output, cell.second != null && !step.switched ? cell.secondPos() : cell.center, motorPos, inputFace, id);
        }
        if (!cell.rotor) DirectionMatrixSupport.check(!first.isRunning() && first.getMovedContraption() == null,
                id + ": static fixture unexpectedly assembled");
        else rotor(level, cell, step, elapsed, evidence);
        return evidence;
    }
    private static void rotor(ServerLevel level, Cell cell, Step step, long elapsed, Map<String, Object> evidence) {
        var bearing = bearing(level, cell.center); var entity = bearing.getMovedContraption();
        if (step.id.equals("disassemble")) {
            DirectionMatrixSupport.check(!bearing.isRunning() && entity == null, "Disassembly retained actual rotor owner");
            DirectionMatrixSupport.check(level.getBlockState(cell.chest()).is(Blocks.CHEST)
                    && level.getBlockState(cell.cutter()).is(OffroadBlocks.ROCK_CUTTER_BLOCK), "Native rotor blocks not restored");
            DirectionMatrixSupport.check(level.getBlockEntity(cell.chest()) instanceof ChestBlockEntity, "Restored chest owner missing");
            var chest = (ChestBlockEntity) level.getBlockEntity(cell.chest());
            DirectionMatrixSupport.check(chest.getItem(0).is(Items.COBBLESTONE) && chest.getItem(0).getCount() == 7,
                    "Real mounted chest inventory changed");
            evidence.put("restoredCobblestone", chest.getItem(0).getCount()); return;
        }
        DirectionMatrixSupport.check(bearing.isRunning() && entity != null, "Genuine borehead rotor has not assembled: " + bearing.getLastAssemblyException());
        var structure = entity.getContraption();
        DirectionMatrixSupport.check(structure != null && structure.rockCuttingWheelAmount == 1 && structure.getBlocks().size() == 2,
                "Actual addon rotor lost chest/cutter or cutter count");
        DirectionMatrixSupport.check(!structure.getStorage().getAllItemStorages().isEmpty()
                && bearing.getContraptionWrappedInventory() != null, "Native mounted inventory missing");
        DirectionMatrixSupport.check(level.getBlockState(cell.chest()).isAir() && level.getBlockState(cell.cutter()).isAir(),
                "Native assembly did not transfer real chest/cutter");
        float angle = bearing.getInterpolatedAngle(1), angularSpeed = bearing.getRotationSpeed();
        DirectionMatrixSupport.check(Float.isFinite(angle) && Float.isFinite(angularSpeed), "Non-finite native rotor angle/speed");
        if (step.id.equals("rotate")) {
            if (cell.rotorUuid == null) { cell.rotorUuid = entity.getUUID(); cell.angleBefore = angle; }
            float distance = Math.abs(((angle - cell.angleBefore + 540) % 360) - 180);
            DirectionMatrixSupport.check(elapsed >= 24 && entity.getUUID().equals(cell.rotorUuid)
                    && distance > 1 && Math.abs(angularSpeed) > .01f, "Actual same-owner rotor has not rotated through 24 ticks");
            evidence.put("angleBefore", cell.angleBefore); evidence.put("angleDistance", distance);
        }
        evidence.put("rotorUuid", entity.getUUID().toString()); evidence.put("rotorBlocks", structure.getBlocks().size());
        evidence.put("rockCuttingWheels", structure.rockCuttingWheelAmount); evidence.put("angle", angle);
        evidence.put("rotationSpeed", angularSpeed); evidence.put("mountedStorage", true);
    }
    private static void checkOrientation(ServerLevel level, BlockPos pos, Orientation o) {
        BlockState state = level.getBlockState(pos);
        DirectionMatrixSupport.check(state.is(OffroadBlocks.BOREHEAD_BEARING_BLOCK)
                && state.getValue(DirectionalAxisKineticBlock.FACING) == o.facing
                && state.getValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE) == o.first,
                "Genuine borehead orientation changed at " + pos + ": " + state);
    }
    private static void checkOwner(ServerLevel level, BlockPos pos, Object expected) {
        DirectionMatrixSupport.check(expected instanceof BoreheadBearingBlockEntity && level.getBlockEntity(pos) == expected,
                "Native borehead owner replaced/missing at " + pos);
    }
    private static BoreheadBearingBlockEntity bearing(ServerLevel level, BlockPos pos) {
        DirectionMatrixSupport.check(level.getBlockEntity(pos) instanceof BoreheadBearingBlockEntity, "Real borehead owner missing at " + pos);
        return (BoreheadBearingBlockEntity) level.getBlockEntity(pos);
    }
    private static KineticBlockEntity kinetic(ServerLevel level, BlockPos pos) {
        DirectionMatrixSupport.check(level.getBlockEntity(pos) instanceof KineticBlockEntity, "Native kinetic owner missing at " + pos);
        return (KineticBlockEntity) level.getBlockEntity(pos);
    }
    private static void speed(KineticBlockEntity owner, float expected, String id) {
        near(owner.getSpeed(), expected, id + " applied RPM"); near(owner.getTheoreticalSpeed(), expected, id + " theoretical RPM");
        DirectionMatrixSupport.check(!owner.isConflictDestructionPending(), id + ": native network conflict pending");
    }
    private static void provenance(KineticBlockEntity owner, BlockPos immediate, BlockPos generator, Direction face, String id) {
        DirectionMatrixSupport.check(immediate.equals(owner.getSource()) && generator.equals(owner.getGeneratingSource())
                && owner.getKineticSourceFace() == face, id + ": native source provenance differs: " + snapshot(owner));
    }
    private static Map<String, Object> snapshot(KineticBlockEntity owner) {
        Map<String, Object> result = new LinkedHashMap<>(); result.put("ownerClass", owner.getClass().getName());
        result.put("rpm", owner.getSpeed()); result.put("theoreticalRpm", owner.getTheoreticalSpeed());
        result.put("source", coordinates(owner.getSource())); result.put("generator", coordinates(owner.getGeneratingSource()));
        result.put("sourceFace", owner.getKineticSourceFace() == null ? null : owner.getKineticSourceFace().getSerializedName());
        return result;
    }
    private static Object coordinates(BlockPos pos) { return pos == null ? null : List.of(pos.getX(), pos.getY(), pos.getZ()); }
    private static void near(float actual, float expected, String id) {
        DirectionMatrixSupport.check(Float.isFinite(actual) && Math.abs(actual - expected) < .01f,
                id + ": expected " + expected + ", got " + actual);
    }
    private DirectionBoreheadMatrix() {}
}
