import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.KineticNetworks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import dev.simulated_team.simulated.content.blocks.analog_transmission.AnalogTransmissionBlockEntity;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.util.extra_kinetics.ExtraBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.TagValueOutput;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** New production AnalogTransmission cases only, driven by real server ticks and vanilla power. */
public final class DirectionAnalogMatrix {
    private static final String GROUP = "analog";
    private static final int BATCH = 24;
    private record Geometry(Direction.Axis axis, boolean extra, Direction meshOrSource, Direction motorSide) {
        String id() { return axis.getSerializedName() + "/" + (extra ? "cog-" : "shaft-") + meshOrSource.getSerializedName() + "/motor-" + motorSide.getSerializedName(); }
    }
    private record Sample(int signal, int rpm, String label, boolean overspeed, boolean parentSource) {
        Sample(int signal, int rpm, String label, boolean overspeed) { this(signal, rpm, label, overspeed, false); }
    }
    private record Case(int index, Geometry geometry, List<Sample> samples, String label) {
        String id() { return geometry.id() + "/" + label; }
    }
    private static final List<Case> CASES = cases();
    private static final List<Fixture> active = new ArrayList<>();
    private static final Map<Long, Integer> chunkLeases = new HashMap<>();
    private static int cursor;
    private static String failureId = "initialize";
    private static boolean announced;

    public static boolean tick(MinecraftServer server, List<String> checks) {
        try {
            var level = DirectionMatrixSupport.level(server);
            if (active.isEmpty()) {
                while (cursor < CASES.size() && active.size() < BATCH) {
                    var test = CASES.get(cursor++);
                    if (complete(test)) continue;
                    var fixture = new Fixture(test, DirectionMatrixSupport.base(GROUP, test.index));
                    failureId = casePhaseId(test);
                    fixture.load(level);
                    active.add(fixture);
                }
                if (active.isEmpty()) {
                    if (!announced) {
                        checks.add("new real AnalogTransmission matrix " + DirectionMatrixSupport.phase() + ": 1440 basic axis/channel/mesh/motor-side/power/signed-RPM cases plus inverse-limit/latch/recovery, both-channel source switches and cold/persisted inverse boundary fixtures; actual vanilla comparator/container power and native persisted owners");
                        announced = true;
                    }
                    return true;
                }
                return false;
            }
            for (var iterator = active.iterator(); iterator.hasNext();) {
                var fixture = iterator.next(); failureId = fixture.id();
                if (fixture.tick(level)) { fixture.release(level); iterator.remove(); }
            }
            return false;
        } catch (Throwable failure) {
            DirectionMatrixSupport.fail(GROUP, failureId, failure);
            if (failure instanceof RuntimeException runtime) throw runtime;
            if (failure instanceof Error error) throw error;
            throw new RuntimeException(failure);
        }
    }

    private static boolean complete(Case test) {
        if (restoring()) return DirectionMatrixSupport.done(GROUP, casePhaseId(test));
        for (var sample : test.samples) if (!DirectionMatrixSupport.done(GROUP, casePhaseId(test) + "/" + sample.label)) return false;
        return true;
    }
    private static String casePhaseId(Case test) { return test.id(); }
    private static boolean restoring() { return DirectionMatrixSupport.phase().equals("restore"); }

    private static List<Case> cases() {
        var geometries = new ArrayList<Geometry>();
        for (var axis : Direction.Axis.values()) {
            for (var source : Direction.values()) if (source.getAxis() == axis) geometries.add(new Geometry(axis, false, source, source));
            for (var mesh : Direction.values()) if (mesh.getAxis() != axis)
                for (var motor : Direction.values()) if (motor.getAxis() == axis) geometries.add(new Geometry(axis, true, mesh, motor));
        }
        var result = new ArrayList<Case>();
        for (var geometry : geometries) for (int signal = 0; signal <= 15; signal++) for (int rpm : new int[]{-8, 0, 8})
            result.add(new Case(result.size(), geometry, List.of(new Sample(signal, rpm, "steady", false)), "power-" + signal + "/rpm-" + rpm));
        if (result.size() != 1440) throw new AssertionError("Analog matrix basic geometry inventory changed");
        for (var geometry : geometries) if (geometry.extra) for (int sign : new int[]{-1, 1})
            result.add(new Case(result.size(), geometry, List.of(new Sample(14, sign * 15, "below-limit", false),
                    new Sample(14, sign * 16, "at-limit", false), new Sample(14, sign * 17, "above-limit", true),
                    new Sample(14, sign * 8, "speed-recovery", false), new Sample(14, sign * 17, "latched-again", true),
                    new Sample(0, sign * 17, "signal-recovery", false)), "inverse-boundary-sign-" + sign));
        for (var geometry : geometries) if (geometry.extra) for (int power : new int[]{0, 7, 14, 15})
            result.add(new Case(result.size(), geometry, List.of(new Sample(power, 8, "extra-positive", false),
                    new Sample(power, 8, "parent-positive", false, true), new Sample(power, -8, "extra-negative", false),
                    new Sample(power, -8, "parent-negative", false, true), new Sample(power, 0, "extra-stopped", false),
                    new Sample(power, 0, "parent-stopped", false, true), new Sample(power, 8, "extra-return", false)), "source-switch-power-" + power));
        // The live boundary sequence ends recovered. These separate fixtures
        // prove genuinely cold over-limit arrival and preserve the latched state
        // itself for the second native JVM, rather than only saving recovery.
        for (var geometry : geometries) if (geometry.extra) for (int sign : new int[]{-1, 1}) for (int rpm : new int[]{15, 16, 17})
            result.add(new Case(result.size(), geometry, List.of(new Sample(14, sign * rpm, "cold-steady", rpm > 16)), "cold-inverse-rpm-" + (sign * rpm)));
        return List.copyOf(result);
    }

    private static final class Fixture {
        final Case test;
        final BlockPos center, cog, motor, parentMotor, comparator, container;
        final Direction powerSide;
        final Set<BlockPos> blocks = new LinkedHashSet<>();
        final Set<Long> chunks = new LinkedHashSet<>();
        int sampleIndex, ticks, stable;
        boolean powerAwakened;
        AnalogTransmissionBlockEntity initialOwner;

        Fixture(Case test, BlockPos center) {
            this.test = test; this.center = center;
            cog = test.geometry.extra ? center.relative(test.geometry.meshOrSource) : null;
            motor = (test.geometry.extra ? cog : center).relative(test.geometry.motorSide);
            blocks.add(center); if (cog != null) blocks.add(cog); blocks.add(motor);
            parentMotor = test.label.startsWith("source-switch") ? center.relative(test.geometry.motorSide) : null;
            if (parentMotor != null) blocks.add(parentMotor);
            Direction selected = null;
            for (var direction : Direction.Plane.HORIZONTAL) {
                var candidate = center.relative(direction);
                if (!blocks.contains(candidate) && !blocks.contains(candidate.below()) && !blocks.contains(candidate.relative(direction))) { selected = direction; break; }
            }
            if (selected == null) throw new AssertionError("No genuine comparator site for " + test.id());
            powerSide = selected; comparator = center.relative(selected); container = comparator.relative(selected);
            blocks.add(comparator); blocks.add(comparator.below()); blocks.add(container);
        }

        String id() { return casePhaseId(test) + (restoring() ? "" : "/" + test.samples.get(sampleIndex).label); }

        void load(ServerLevel level) {
            for (var pos : blocks) chunks.add(chunkKey(pos));
            for (long chunk : chunks) {
                int leases = chunkLeases.getOrDefault(chunk, 0);
                if (leases == 0) level.setChunkForced((int) (chunk >> 32), (int) chunk, true);
                chunkLeases.put(chunk, leases + 1);
            }
            for (var pos : blocks) level.getChunk(pos);
            if (!restoring()) {
                while (sampleIndex + 1 < test.samples.size() && DirectionMatrixSupport.done(GROUP, casePhaseId(test) + "/" + test.samples.get(sampleIndex).label)) sampleIndex++;
                // Generated structures can exist above a superflat floor. A
                // blocked vanilla chest correctly gives comparator strength0,
                // so isolate this new fixture before authoring its real owners.
                for (var pos : BlockPos.betweenClosed(center.offset(-3, -2, -3), center.offset(3, 3, 3)))
                    if (!level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                for (var pos : blocks) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(comparator.below(), Blocks.STONE.defaultBlockState(), 3);
                level.setBlock(container, Blocks.CHEST.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, powerSide), 3);
                level.setBlock(comparator, Blocks.COMPARATOR.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, powerSide), 3);
                level.setBlock(center, SimBlocks.ANALOG_TRANSMISSION.defaultBlockState().setValue(BlockStateProperties.AXIS, test.geometry.axis), 3);
                if (cog != null) level.setBlock(cog, AllBlocks.COGWHEEL.defaultBlockState().setValue(BlockStateProperties.AXIS, test.geometry.axis), 3);
                level.setBlock(motor, AllBlocks.CREATIVE_MOTOR.defaultBlockState().setValue(BlockStateProperties.FACING, test.geometry.motorSide.getOpposite()), 3);
                initialOwner = requireAnalog(level);
                apply(level);
            } else sampleIndex = test.samples.size() - 1;
        }

        void apply(ServerLevel level) {
            var sample = test.samples.get(sampleIndex);
            if (parentMotor != null) {
                check(requireAnalog(level) == initialOwner, id() + " source switching replaced the actual paid transmission owner");
                level.setBlock(motor, Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(parentMotor, Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(activeMotor(), AllBlocks.CREATIVE_MOTOR.defaultBlockState().setValue(BlockStateProperties.FACING, test.geometry.motorSide.getOpposite()), 3);
            }
            var source = requireMotor(level);
            int dial = sample.rpm * source.getBlockState().getValue(BlockStateProperties.FACING).getAxisDirection().getStep();
            source.generatedSpeed.setValue(dial);
            var chest = (ChestBlockEntity) level.getBlockEntity(container);
            check(chest != null, "Vanilla signal container missing");
            chest.clearContent();
            int count = sample.signal == 0 ? 0 : sample.signal == 15 ? 27 * 64 : (int) Math.floor((sample.signal - .5) * (27 * 64) / 14.0);
            for (int slot = 0; slot < 27 && count > 0; slot++) { int stack = Math.min(64, count); chest.setItem(slot, new ItemStack(Items.COBBLESTONE, stack)); count -= stack; }
            chest.setChanged(); level.updateNeighborsAt(container, Blocks.CHEST);
            level.updateNeighborsAt(comparator, Blocks.COMPARATOR);
            ticks = stable = 0; powerAwakened = false;
        }

        boolean tick(ServerLevel level) {
            ticks++;
            if (!powerAwakened) {
                if (!blocks.stream().allMatch(level::isPositionEntityTicking)) {
                    check(ticks < 200, id() + " fixture chunk never entered actual ticking state");
                    return false;
                }
                // A comparator placed before its newly forced chunk starts ticking
                // can lose its first scheduled update. Wake the genuine vanilla
                // calculation after the real world reports all owners active.
                level.updateNeighborsAt(container, Blocks.CHEST);
                level.scheduleTick(comparator, Blocks.COMPARATOR, 2);
                powerAwakened = true; ticks = 0;
                return false;
            }
            if (ticks < 30) return false;
            var sample = test.samples.get(sampleIndex);
            var parent = requireAnalog(level); var secondary = parent.getExtraKinetics(); var source = requireMotor(level);
            int signal = level.getBestNeighborSignal(center);
            double q = sample.signal == 0 ? 1 : (15 - sample.signal) / 16.0;
            boolean extraSource = test.geometry.extra && !sample.parentSource;
            double expectedParent = extraSource ? sample.signal == 15 || sample.overspeed ? 0 : -sample.rpm / q : sample.rpm;
            double expectedExtra = extraSource ? -sample.rpm : sample.signal == 15 ? 0 : sample.rpm * q;
            boolean matches = signal == sample.signal && close(source.getSpeed(), sample.rpm)
                    && close(parent.getSpeed(), expectedParent) && close(secondary.getSpeed(), expectedExtra)
                    && oversaturated(parent) == sample.overspeed;
            if (!matches) { stable = 0; if (ticks < 100) return false; }
            else if (++stable < 3) return false;
            var evidence = snapshot(level, parent, secondary, source, sample, signal, expectedParent, expectedExtra);
            check(matches, id() + " native analog expectation mismatch: " + evidence);
            check(!parent.isConflicted() && !secondary.isConflicted() && !source.isConflicted(), id() + " native analog network conflicted: " + evidence);
            if (sample.rpm != 0 && sample.signal < 15 && !sample.overspeed) {
                check(parent.getSource() != null && secondary.getSource() != null, id() + " native source provenance disappeared: " + evidence);
                check(extraSource == (parent.getSource() instanceof ExtraBlockPos), id() + " parent source channel marker does not follow the real powered channel: " + evidence);
                check(!(secondary.getSource() instanceof ExtraBlockPos), id() + " secondary inherited an invalid extra source marker: " + evidence);
            }
            DirectionMatrixSupport.pass(GROUP, id(), evidence);
            if (restoring() || ++sampleIndex >= test.samples.size()) return true;
            apply(level); return false;
        }

        Map<String, Object> snapshot(ServerLevel level, AnalogTransmissionBlockEntity parent, KineticBlockEntity child, CreativeMotorBlockEntity source,
                                     Sample sample, int actualSignal, double expectedParent, double expectedExtra) {
            var result = new LinkedHashMap<String, Object>();
            result.put("center", List.of(center.getX(), center.getY(), center.getZ())); result.put("axis", test.geometry.axis.getSerializedName());
            result.put("route", test.geometry.extra && !sample.parentSource ? "actual-small-cog" : "actual-parent-shaft");
            result.put("meshOrSourceFace", test.geometry.meshOrSource.getSerializedName()); result.put("motorSide", test.geometry.motorSide.getSerializedName());
            result.put("power", sample.signal); result.put("actualNativeNeighborPower", actualSignal); result.put("motorRpm", source.getSpeed());
            result.put("actualParentRpm", parent.getSpeed()); result.put("expectedParentRpm", expectedParent);
            result.put("actualExtraRpm", child.getSpeed()); result.put("expectedExtraRpm", expectedExtra);
            result.put("oversaturated", oversaturated(parent)); result.put("ticksSettled", ticks); result.put("phase", DirectionMatrixSupport.phase());
            result.put("comparatorState", level.getBlockState(comparator).toString());
            if (level.getBlockEntity(comparator) instanceof ComparatorBlockEntity vanilla) result.put("nativeComparatorOutput", vanilla.getOutputSignal());
            if (level.getBlockEntity(container) instanceof ChestBlockEntity vanilla) {
                int count = 0; for (int slot = 0; slot < vanilla.getContainerSize(); slot++) count += vanilla.getItem(slot).getCount();
                result.put("nativeSignalChestItems", count);
                result.put("nativeContainerFullnessPower", AbstractContainerMenu.getRedstoneSignalFromContainer(vanilla));
            }
            result.put("signalContainerState", level.getBlockState(container).toString());
            try {
                for (String methodName : List.of("getInputSignal", "calculateOutputSignal")) {
                    var method = ComparatorBlock.class.getDeclaredMethod(methodName, Level.class, BlockPos.class, BlockState.class);
                    method.setAccessible(true);
                    result.put("nativeComparator-" + methodName, method.invoke(Blocks.COMPARATOR, level, comparator, level.getBlockState(comparator)));
                }
            } catch (ReflectiveOperationException diagnostic) { result.put("comparatorDiagnostic", diagnostic.toString()); }
            result.put("actualBlockEntityType", BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(parent.getType()).toString());
            result.put("parentSource", sourceDescription(parent)); result.put("extraSource", sourceDescription(child));
            if (initialOwner != null) result.put("samePaidOwnerWithinSequence", parent == initialOwner);
            return result;
        }

        void release(ServerLevel level) {
            for (long chunk : chunks) {
                int leases = chunkLeases.get(chunk) - 1;
                if (leases == 0) { chunkLeases.remove(chunk); level.setChunkForced((int) (chunk >> 32), (int) chunk, false); }
                else chunkLeases.put(chunk, leases);
            }
        }
        AnalogTransmissionBlockEntity requireAnalog(ServerLevel level) {
            var entity = level.getBlockEntity(center);
            check(entity instanceof AnalogTransmissionBlockEntity, id() + " actual paid AnalogTransmission owner disappeared: " + entity + " state=" + level.getBlockState(center));
            return (AnalogTransmissionBlockEntity) entity;
        }
        CreativeMotorBlockEntity requireMotor(ServerLevel level) {
            var position = activeMotor();
            var entity = level.getBlockEntity(position);
            check(entity instanceof CreativeMotorBlockEntity, id() + " actual motor owner disappeared: " + entity + " state=" + level.getBlockState(position));
            return (CreativeMotorBlockEntity) entity;
        }
        BlockPos activeMotor() { return parentMotor != null && test.samples.get(sampleIndex).parentSource ? parentMotor : motor; }
    }

    private static String sourceDescription(KineticBlockEntity owner) {
        var source = owner.getSource();
        return source == null ? "none" : source.getClass().getSimpleName() + "@" + source.getX() + "," + source.getY() + "," + source.getZ();
    }
    private static boolean oversaturated(AnalogTransmissionBlockEntity owner) {
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, owner.getLevel().registryAccess());
        owner.saveWithoutMetadata(output); return output.buildResult().getBooleanOr("Oversaturated", false);
    }
    private static boolean close(double actual, double expected) { return Double.isFinite(actual) && Math.abs(actual - expected) < 1E-3; }
    private static long chunkKey(BlockPos pos) { return ((long) (pos.getX() >> 4) << 32) | ((pos.getZ() >> 4) & 0xffffffffL); }
    private static void check(boolean value, String message) { DirectionMatrixSupport.check(value, message); }
}
