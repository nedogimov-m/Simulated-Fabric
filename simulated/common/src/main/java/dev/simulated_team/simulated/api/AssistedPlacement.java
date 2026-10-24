package dev.simulated_team.simulated.api;

import com.simibubi.create.foundation.utility.LoadedLevelAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/** Server-safe placement geometry shared by real BlockItem actions and extracted client previews. */
public final class AssistedPlacement {
    private static final List<Helper> HELPERS = new ArrayList<>();
    public static synchronized int register(final Helper helper) { HELPERS.add(Objects.requireNonNull(helper)); return HELPERS.size() - 1; }
    public static synchronized Helper get(final int id) { return HELPERS.get(id); }
    public static synchronized List<Helper> helpers() { return List.copyOf(HELPERS); }
    public interface Helper {
        Predicate<ItemStack> getItemPredicate();
        Predicate<BlockState> getStatePredicate();
        Offset getOffset(Player player, Level level, BlockState state, BlockPos pos, BlockHitResult hit);
        default Offset getOffset(final Player player, final Level level, final BlockState state, final BlockPos pos,
                                 final BlockHitResult hit, final ItemStack held) { return this.getOffset(player, level, state, pos, hit); }
        default boolean matchesItem(final ItemStack stack) { return this.getItemPredicate().test(stack); }
        default boolean matchesState(final BlockState state) { return this.getStatePredicate().test(state); }
        static List<Direction> orderedByDistance(final BlockPos pos, final Vec3 hit, final Predicate<Direction> filter) {
            return Arrays.stream(Direction.values()).filter(filter).sorted(Comparator.comparingDouble(direction ->
                    Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(direction.getStep()).scale(.5)).distanceToSqr(hit))).toList();
        }
        static List<Direction> orderedByDistanceExceptAxis(final BlockPos pos, final Vec3 hit, final Direction.Axis axis) {
            return orderedByDistance(pos, hit, direction -> direction.getAxis() != axis);
        }
        static List<Direction> orderedByDistanceExceptAxis(final BlockPos pos, final Vec3 hit, final Direction.Axis axis,
                                                          final Predicate<Direction> filter) {
            return orderedByDistance(pos, hit, direction -> direction.getAxis() != axis && filter.test(direction));
        }
    }
    public record Offset(@Nullable BlockPos target, UnaryOperator<BlockState> transform) {
        public Offset { if (target != null) target = target.immutable(); Objects.requireNonNull(transform); }
        public static Offset fail() { return new Offset(null, UnaryOperator.identity()); }
        public static Offset success(final BlockPos target, final UnaryOperator<BlockState> transform) { return new Offset(target, transform); }
        public boolean successful() { return this.target != null; }
        public InteractionResult placeInWorld(final Level level, final BlockItem item, final Player player,
                                              final InteractionHand hand, final BlockHitResult sourceHit) {
            if (!this.successful()) return InteractionResult.TRY_WITH_EMPTY_HAND;
            final ItemStack stack = player.getItemInHand(hand);
            if (player.isShiftKeyDown() || player.isSpectator() || !player.mayBuild()
                    || !LoadedLevelAccess.isLoaded(level, this.target) || !level.isInValidBounds(this.target)
                    || !level.getWorldBorder().isWithinBounds(this.target) || !level.mayInteract(player, this.target)
                    || !player.mayUseItemAt(this.target, sourceHit.getDirection(), stack)) return InteractionResult.FAIL;
            final var context = context(new UseOnContext(player, hand, sourceHit), this);
            if (!context.getClickedPos().equals(this.target) || !context.canPlace()) return InteractionResult.FAIL;
            final InteractionResult result = item.place(context);
            if (!level.isClientSide() && result.consumesAction()) player.awardStat(Stats.ITEM_USED.get(item));
            return result;
        }
    }
    public static BlockPlaceContext context(final UseOnContext source, final Offset offset) {
        if (!offset.successful()) throw new IllegalArgumentException("No assisted placement target");
        return new Context(source, offset);
    }
    /** Invoked before native BlockItem.canPlace validation, costs, components and callbacks. */
    public static @Nullable BlockState transform(final BlockPlaceContext context, final @Nullable BlockState state) {
        return state != null && context instanceof final Context assisted ? assisted.offset.transform.apply(state) : state;
    }
    public static boolean isAssistedPlacement(final BlockPlaceContext context) { return context instanceof Context; }
    private static final class Context extends BlockPlaceContext {
        private final Offset offset;
        private Context(final UseOnContext source, final Offset offset) {
            super(source.getLevel(), source.getPlayer(), source.getHand(), source.getItemInHand(),
                    new BlockHitResult(Vec3.atCenterOf(offset.target), source.getClickedFace(), offset.target, false));
            this.offset = offset;
        }
    }
    private AssistedPlacement() { }
}
