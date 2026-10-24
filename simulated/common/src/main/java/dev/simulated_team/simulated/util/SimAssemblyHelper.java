package dev.simulated_team.simulated.util;

import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.OrientedContraptionEntity;
import com.simibubi.create.content.contraptions.StabilizedSubContraptions;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot;
import dev.ryanhcode.sable.sublevel.plot.heat.SubLevelHeatMapManager;
import dev.simulated_team.simulated.content.entities.honey_glue.HoneyGlueEntity;
import dev.simulated_team.simulated.index.SimSoundEvents;
import dev.simulated_team.simulated.mixin.accessor.ContraptionAccessor;
import dev.simulated_team.simulated.mixin_interface.create_assembly.IControlContraptionExtension;
import dev.simulated_team.simulated.util.assembly.SimAssemblyContraption;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import com.simibubi.create.foundation.utility.LoadedLevelAccess;
import java.util.stream.Stream;

public class SimAssemblyHelper {
    public static boolean disassembleSubLevel(@NotNull final Level level,
                                           @NotNull final SubLevel toDisassemble,
                                           @NotNull final BlockPos subLevelAnchor,
                                           @NotNull final BlockPos disassemblyGoal,
                                           @NotNull final Rotation rotation,
                                           @NotNull final boolean playSound) {
        return disassembleSubLevel(level, toDisassemble, subLevelAnchor, disassemblyGoal, rotation, playSound, () -> {});
    }

    public static boolean disassembleSubLevel(@NotNull final Level level,
                                              @NotNull final SubLevel toDisassemble,
                                              @NotNull final BlockPos subLevelAnchor,
                                              @NotNull final BlockPos disassemblyGoal,
                                              @NotNull final Rotation rotation,
                                              final boolean playSound,
                                              @NotNull final Runnable beforeMove) {
        final SubLevelAssemblyHelper.AssemblyTransform transform = new SubLevelAssemblyHelper.AssemblyTransform(subLevelAnchor, disassemblyGoal, rotation == Rotation.NONE ? 0 : (4 - rotation.ordinal()), rotation, (ServerLevel) level);

        final ObjectArrayList<BlockPos> blocks = new ObjectArrayList<>();
        final LevelPlot plot = toDisassemble.getPlot();
        for (final PlotChunkHolder chunk : plot.getLoadedChunks()) {
            final BoundingBox3ic localChunkBounds = chunk.getBoundingBox();

            if (localChunkBounds == null || localChunkBounds == BoundingBox3i.EMPTY) {
                continue;
            }

            for (int x = localChunkBounds.minX(); x <= localChunkBounds.maxX(); x++) {
                for (int y = localChunkBounds.minY(); y <= localChunkBounds.maxY(); y++) {
                    for (int z = localChunkBounds.minZ(); z <= localChunkBounds.maxZ(); z++) {
                        final BlockPos pos = new BlockPos(
                                x + chunk.getPos().getMinBlockX(),
                                y,
                                z + chunk.getPos().getMinBlockZ()
                        );
                        final BlockState state = level.getBlockState(pos);
                        if (!state.isAir()) {
                            blocks.add(pos);
                        }
                    }
                }
            }
        }

        if (!disassembleAndAddCreateContraptions(level, plot.getBoundingBox(), blocks, false, null)) return false;
        final BoundingBox3i plotBounds = blocks.isEmpty() ? new BoundingBox3i(plot.getBoundingBox()) : BoundingBox3i.from(blocks);
        beforeMove.run();
        blocks.removeIf(pos -> LoadedLevelAccess.blockState(level, pos).isAir());

        if (playSound) {
            level.playSound(null, subLevelAnchor, SimSoundEvents.SIMULATED_CONTRAPTION_STOPS.event(), SoundSource.BLOCKS, 1.0f, 1.0f);
        }

        // move glue
        final PersistentEntitySectionManager<Entity> manager = ((ServerLevel) toDisassemble.getLevel()).entityManager;
        for (final PlotChunkHolder chunk : toDisassemble.getPlot().getLoadedChunks()) {
            final Stream<EntitySection<Entity>> sections = manager.sectionStorage.getExistingSectionsInChunk(chunk.getPos().pack());

            for (final EntitySection<Entity> section : sections.toList()) {
                final List<Entity> entities = section.getEntities().toList();

                for (final Entity entity : entities) {
                    AABB box = entity.getBoundingBox();

                    box = new AABB(transform.apply(new Vec3(box.minX, box.minY, box.minZ)),
                            transform.apply(new Vec3(box.maxX, box.maxY, box.maxZ)));

                    if (entity instanceof SuperGlueEntity) {
                        entity.remove(Entity.RemovalReason.KILLED);
                        level.addFreshEntity(new SuperGlueEntity(level, box));
                        continue;
                    }

                    if (entity instanceof HoneyGlueEntity) {
                        entity.remove(Entity.RemovalReason.KILLED);
                        final HoneyGlueEntity newHoneyGlue = new HoneyGlueEntity(level, box);
                        level.addFreshEntity(newHoneyGlue);
                        newHoneyGlue.setBoundsAndSync(box);
                        continue;
                    }

                    final Vec3 newPos = transform.apply(entity.position());
                    entity.setPos(newPos);
                    entity.setYRot(entity.rotate(transform.getRotation()));
                    entity.yRotO = entity.getYRot();

                    if (entity instanceof final HangingEntity hangingEntity) {
                        hangingEntity.recalculateBoundingBox();
                    }

                    entity.levelCallback.onRemove(Entity.RemovalReason.CHANGED_DIMENSION);
                    ((ServerLevel) level).addDuringTeleport(entity);
                }
            }
        }

        // if there's no blocks in the given sublevel, don't attempt to move the blocks
        if (!blocks.isEmpty()) {
            ((ServerLevelPlot) toDisassemble.getPlot()).kickAllEntities();
            SubLevelAssemblyHelper.moveBlocks((ServerLevel) level, transform, blocks);
        }

        SubLevelAssemblyHelper.moveTrackingPoints((ServerLevel) level, plotBounds, null, transform);
        return true;
    }

    public static AssemblyResult assembleFromSingleBlock(final Level level, final BlockPos selfPos, final BlockPos toAssemble, final boolean includeStart, final boolean includeEncasingGlue) throws AssemblyException {
        if (level.getBlockState(toAssemble).isAir()) {
            return null;
        }

        final SimAssemblyContraption contraption = new SimAssemblyContraption(includeStart ? null : selfPos, !includeEncasingGlue);

        contraption.searchMovedStructure(level, toAssemble);

        final Collection<BlockPos> blocks = contraption.getBlocks();
        if (!blocks.isEmpty()) {
            BoundingBox3i bounds = BoundingBox3i.from(blocks);
            final Collection<SuperGlueEntity> superGlues = contraption.getGlues();
            final Collection<HoneyGlueEntity> honeyGlues = contraption.getHoneyGlues();

            final ObjectArrayList<AABB> collectedContraptionGlues = new ObjectArrayList<>();
            if (!disassembleAndAddCreateContraptions(level, bounds, blocks, true, collectedContraptionGlues)) return null;
            bounds = BoundingBox3i.from(blocks);

            final BlockPos anchor = blocks.stream().findFirst().get();
            final SubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks((ServerLevel) level, anchor, blocks, bounds);

            if (subLevel != null) {
                final BlockPos offsetBlocks = subLevel.getPlot().getCenterBlock().subtract(anchor);

                // move glues from contraptions
                for (final AABB box : collectedContraptionGlues) {
                    level.addFreshEntity(new SuperGlueEntity(level, box.move(Vec3.atLowerCornerOf(offsetBlocks))));
                }

                for (final SuperGlueEntity glue : superGlues) {
                    glue.remove(Entity.RemovalReason.KILLED);
                    level.addFreshEntity(new SuperGlueEntity(level, glue.getBoundingBox().move(Vec3.atLowerCornerOf(offsetBlocks))));
                }

                for (final HoneyGlueEntity glue : honeyGlues) {
                    glue.remove(Entity.RemovalReason.KILLED);
                    final AABB newBB = glue.getBoundingBox().move(Vec3.atLowerCornerOf(offsetBlocks));
                    final HoneyGlueEntity entity = new HoneyGlueEntity(level, newBB);
                    level.addFreshEntity(entity);
                    entity.setBoundsAndSync(newBB);
                }

                level.playSound(null, selfPos, SimSoundEvents.SIMULATED_CONTRAPTION_MOVES.event(), SoundSource.BLOCKS, 1.0f, 1.0f);
                return new AssemblyResult(subLevel, offsetBlocks);
            }
            // Native restoration has already succeeded; a refused Sable allocation must leave
            // the detached glue beside its restored source blocks instead of losing it.
            for (final AABB box : collectedContraptionGlues) level.addFreshEntity(new SuperGlueEntity(level, box));
        }

        return null;
    }

    private static boolean disassembleAndAddCreateContraptions(final Level level, final BoundingBox3ic assemblyBounds, final Collection<BlockPos> blocks, final boolean passGluesBack, final List<AABB> collectedGlues) {
        assert assemblyBounds != null;

        final AABB assemblyBoundsD = new AABB(
                assemblyBounds.minX(), assemblyBounds.minY(), assemblyBounds.minZ(),
                assemblyBounds.maxX() + 1, assemblyBounds.maxY() + 1, assemblyBounds.maxZ() + 1
        );

        // disassemble all of the contraptions controlled by these blocks
        // and add their blocks to be moved as-well
        final List<ControlledContraptionEntity> remaining = new ArrayList<>(level.getEntitiesOfClass(ControlledContraptionEntity.class, assemblyBoundsD.inflate(2.0)));
        final Set<BlockPos> completedControllers = new HashSet<>();
        final Set<BlockPos> acceptedBlocks = new LinkedHashSet<>(blocks);
        final List<AABB> pendingGlues = new ArrayList<>();
        boolean complete = false;
        try {
            // Check all currently selected controllers before the first restoration mutates the
            // world. Newly restored nested controllers are checked when they enter the move set.
            try {
                for (final ControlledContraptionEntity owner : remaining) {
                    final BlockPos controllerPos = owner.getControllerPos();
                    if (controllerPos == null || !acceptedBlocks.contains(controllerPos) || owner.getContraption() == null || owner.getContraption().disassembled) continue;
                    if (!LoadedLevelAccess.isLoaded(level, controllerPos)) return false;
                    final BlockEntity controller = LoadedLevelAccess.blockEntity(level, controllerPos);
                    final IControlContraptionExtension control = controller instanceof IControlContraptionExtension extension ? extension : null;
                    for (final RestorationSnapshot snapshot : snapshotForest((ServerLevel) level, List.of(owner)))
                        snapshot.structure.validateRestoration(level, snapshot.transform(control));
                }
            } catch (AssemblyException unavailable) { return false; }
            boolean progress;
            do {
                progress = false;
                for (final ControlledContraptionEntity entity : List.copyOf(remaining)) {
                    final BlockPos controllerPos = entity.getControllerPos();
                    if (controllerPos == null || !acceptedBlocks.contains(controllerPos) || completedControllers.contains(controllerPos)) continue;
                    if (!LoadedLevelAccess.isLoaded(level, controllerPos)) return false;
                    final BlockEntity controller = LoadedLevelAccess.blockEntity(level, controllerPos);
                    final IControlContraptionExtension control = controller instanceof IControlContraptionExtension extension ? extension : null;
                    // A clock owns two real entities. Its native controller restores the whole pair once.
                    final List<RestorationSnapshot> group;
                    try {
                        group = snapshotForest((ServerLevel) level, remaining.stream()
                                .filter(owner -> controllerPos.equals(owner.getControllerPos()) && owner.getContraption() != null && !owner.getContraption().disassembled).toList());
                        for (final RestorationSnapshot snapshot : group)
                            snapshot.structure.validateRestoration(level, snapshot.transform(control));
                    } catch (AssemblyException unavailable) { return false; }
                    if (passGluesBack) group.forEach(RestorationSnapshot::detachGlue);
                    boolean restored = false;
                    try {
                        restored = control != null ? control.sable$disassemble() : disassembleDirectly(group);
                        restored &= group.stream().allMatch(snapshot -> snapshot.structure.disassembled);
                    } finally {
                        if (!restored && passGluesBack) {
                            // A paired controller can restore one owner before another refuses. Keep glue
                            // with a still-live cargo owner, or materialize it at its committed world pose.
                            for (final RestorationSnapshot snapshot : group) snapshot.restoreGlueAfterRefusal(level, control);
                        }
                    }
                    if (!restored) return false;
                    for (final RestorationSnapshot snapshot : group) {
                        final StructureTransform transform = snapshot.transform(control);
                        for (final BlockPos local : snapshot.blocks) acceptedBlocks.add(transform.apply(local));
                        if (passGluesBack) for (final AABB glue : snapshot.glue) pendingGlues.add(transformBox(transform, glue));
                    }
                    remaining.removeIf(owner -> controllerPos.equals(owner.getControllerPos()));
                    completedControllers.add(controllerPos);
                    progress = true;
                }
            } while (progress);
            for (final BlockPos committed : acceptedBlocks) if (!blocks.contains(committed)) blocks.add(committed);
            if (passGluesBack) collectedGlues.addAll(pendingGlues);
            complete = true;
            return true;
        } finally {
            if (!complete) for (final AABB glue : pendingGlues) level.addFreshEntity(new SuperGlueEntity(level, glue));
        }
    }

    private static boolean disassembleDirectly(final List<RestorationSnapshot> group) {
        for (final RestorationSnapshot snapshot : group) if (snapshot.parent == null && !snapshot.entity.disassemble()) return false;
        return true;
    }

    private static List<RestorationSnapshot> snapshotForest(final ServerLevel level, final List<ControlledContraptionEntity> roots) throws AssemblyException {
        final List<RestorationSnapshot> snapshots = new ArrayList<>();
        final Set<UUID> visited = new HashSet<>();
        for (final ControlledContraptionEntity root : roots) snapshotTree(level, root, null, null, snapshots, visited, 0);
        return snapshots;
    }

    private static void snapshotTree(final ServerLevel level, final AbstractContraptionEntity owner,
                                     final RestorationSnapshot parent, final StabilizedSubContraptions.Location location,
                                     final List<RestorationSnapshot> snapshots, final Set<UUID> visited, final int depth) throws AssemblyException {
        // Capture bookkeeping only. Native Create still validates, places and retires the forest.
        // Bound traversal by the same native ownership limits and resolve only real loaded UUIDs.
        if (depth > 64 || !visited.add(owner.getUUID()) || visited.size() > 1024) throw AssemblyException.structureTooLarge();
        if (owner.getContraption() == null) throw AssemblyException.entitiesNotReady(owner.blockPosition());
        final RestorationSnapshot snapshot = new RestorationSnapshot(owner, parent, location);
        for (final var child : owner.getContraption().getStabilizedSubContraptions().children().entrySet()) {
            final OrientedContraptionEntity resolved = owner.getContraption().getStabilizedSubContraptions().resolve(level, child.getKey());
            snapshotTree(level, resolved, snapshot, child.getValue(), snapshots, visited, depth + 1);
        }
        snapshots.add(snapshot);
    }

    private static AABB transformBox(final StructureTransform transform, final AABB box) {
        return new AABB(transform.apply(new Vec3(box.minX, box.minY, box.minZ)), transform.apply(new Vec3(box.maxX, box.maxY, box.maxZ)));
    }

    private static final class RestorationSnapshot {
        final AbstractContraptionEntity entity;
        final RestorationSnapshot parent;
        final StabilizedSubContraptions.Location location;
        final Contraption structure;
        final List<BlockPos> blocks;
        final List<AABB> glue;
        RestorationSnapshot(final AbstractContraptionEntity entity, final RestorationSnapshot parent, final StabilizedSubContraptions.Location location) {
            this.entity = entity;
            this.parent = parent;
            this.location = location;
            this.structure = entity.getContraption();
            this.blocks = List.copyOf(structure.getBlocks().keySet());
            this.glue = List.copyOf(((ContraptionAccessor) structure).getSuperGlue());
        }
        StructureTransform transform(final IControlContraptionExtension control) {
            if (parent != null) return ((OrientedContraptionEntity) entity).restorationAt(parent.transform(control).apply(location.connected()));
            return control == null ? entity.getRestorationTransform() : control.sable$restorationTransform((ControlledContraptionEntity) entity);
        }
        void detachGlue() { ((ContraptionAccessor) structure).getSuperGlue().clear(); }
        void restoreGlueAfterRefusal(final Level level, final IControlContraptionExtension control) {
            if (!structure.disassembled) ((ContraptionAccessor) structure).getSuperGlue().addAll(glue);
            else for (final AABB box : glue) level.addFreshEntity(new SuperGlueEntity(level, transformBox(transform(control), box)));
        }
    }

    public static Rotation rotationFrom90DegRots(final int rots) {
        return switch (Math.floorMod(rots, 4)) {
            case 0 -> Rotation.NONE;
            case 1 -> Rotation.COUNTERCLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.CLOCKWISE_90;
            default -> throw new AssertionError(); // unreachable
        };
    }

    public static void register() {
        SubLevelHeatMapManager.addSplitPreparation(SimAssemblyHelper::addSplitBlocks);
    }

    private static boolean addSplitBlocks(final Level level, final BoundingBox3ic boundingBox3ic, final Collection<BlockPos> blocks) {
        return disassembleAndAddCreateContraptions(level, boundingBox3ic, blocks, false, null);
    }

    public record AssemblyResult(SubLevel subLevel, BlockPos offset) { }
}
