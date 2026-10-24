package dev.eriksonn.aeronautics.api.levitite_blend_crystallization;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import java.util.*;

public final class LevititeCrystallizerManager {
    private static final Map<LevelAccessor, List<LevititeBlendTicker>> tickers = new IdentityHashMap<>();
    private static final Map<LevelAccessor, List<LevititeBlendTicker>> queuedTickers = new IdentityHashMap<>();

    public static void tick(final Level level) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        // Load before advancing: an unloaded save must never replace a newly queued world state.
        final var data = CrystallizationWorldSaveData.get(serverLevel);
        final var levelTickers = tickers.get(level);
        if (levelTickers != null && !levelTickers.isEmpty()) {
            levelTickers.removeIf(LevititeBlendTicker::tick);
            // Age, attempts and removals all affect the persisted state.
            data.setDirty();
        }
        addQueued(level, data);
    }

    private static void addQueued(final Level level, final CrystallizationWorldSaveData data) {
        // Detach first, so callbacks may safely enqueue work for the next tick.
        final var queued = queuedTickers.remove(level);
        if (queued == null || queued.isEmpty()) return;
        final Set<BlockPos> tickedPositions = getTickedPositions(level);
        final var levelTickers = tickers.computeIfAbsent(level, ignored -> new ArrayList<>());
        for (final var queuedTicker : queued) {
            if (!tickedPositions.add(queuedTicker.getPos())) continue;
            levelTickers.add(queuedTicker);
            data.setDirty();
            queuedTicker.getContext().onCrystallizationInitialize(level, queuedTicker.getPos(), queuedTicker.isDormant);
        }
    }

    /** Add a new ticker; each world drains its own queue after its active tickers finish. */
    public static void addTicker(final Level level, final BlockPos pos, final int delay,
                                 final boolean requiresCatalyst, final boolean isDormant,
                                 final CrystalPropagationContext context) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        final var data = CrystallizationWorldSaveData.get(serverLevel);
        queuedTickers.computeIfAbsent(level, ignored -> new ArrayList<>())
                .add(new LevititeBlendTicker(delay, pos, level, requiresCatalyst, isDormant, context));
        data.setDirty();
    }

    public static boolean hasTicker(final Level level, final BlockPos pos) {
        return tickers.getOrDefault(level, List.of()).stream().anyMatch(ticker -> ticker.getPos().equals(pos))
                || queuedTickers.getOrDefault(level, List.of()).stream().anyMatch(ticker -> ticker.getPos().equals(pos));
    }

    public static Set<BlockPos> getTickedPositions(final Level level) {
        final Set<BlockPos> positions = new HashSet<>();
        final var active = tickers.get(level);
        if (active != null) active.forEach(ticker -> positions.add(ticker.getPos()));
        return positions;
    }

    public static void saveData(final ListTag list, final Level level) {
        final Set<BlockPos> positions = new HashSet<>();
        // A save between enqueue and END_WORLD_TICK must retain the queued entries too.
        for (final var source : List.of(tickers.getOrDefault(level, List.of()),
                                       queuedTickers.getOrDefault(level, List.of()))) {
            for (final var ticker : source) {
                if (positions.add(ticker.getPos())) list.add(ticker.serialize());
            }
        }
    }

    public static void loadData(final CompoundTag tag, final Level level) {
        final var loaded = new ArrayList<LevititeBlendTicker>();
        final Set<BlockPos> positions = new HashSet<>();
        for (final var entry : tag.getListOrEmpty("Levitite Manager Data")) {
            if (!(entry instanceof CompoundTag compound)) continue;
            final var ticker = new LevititeBlendTicker(compound, level);
            if (positions.add(ticker.getPos())) loaded.add(ticker);
        }
        tickers.put(level, loaded);
    }

    /** Flush while the live lists are still reachable; native storage may also close after UNLOAD. */
    public static void saveAndClearLevel(final ServerLevel level) {
        level.getDataStorage().saveAndJoin();
        clearLevel(level);
    }

    public static void clearLevel(final LevelAccessor level) {
        tickers.remove(level);
        queuedTickers.remove(level);
    }

    private LevititeCrystallizerManager() {}
}
