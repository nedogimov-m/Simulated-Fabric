package dev.simulated_team.simulated.util.placement_helpers;

import com.simibubi.create.foundation.utility.LoadedLevelAccess;
import dev.simulated_team.simulated.api.AssistedPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Function;
import java.util.function.Predicate;

/** Bounded nonloading extension of a pole with the clicked pole's axis/property. */
public abstract class PolePlacementHelper<T extends Comparable<T>> implements AssistedPlacement.Helper {
    private final Predicate<BlockState> extendsPole;
    private final Function<BlockState, T> value;
    private final Property<T> property;
    protected PolePlacementHelper(final Predicate<BlockState> extendsPole, final Function<BlockState, T> value, final Property<T> property) {
        this.extendsPole = extendsPole; this.value = value; this.property = property;
    }
    @Override public AssistedPlacement.Offset getOffset(final Player player, final Level level, final BlockState state,
                                                        final BlockPos pos, final BlockHitResult hit) {
        final T propertyValue = this.value.apply(state);
        if (!(propertyValue instanceof final Direction.Axis axis)) return AssistedPlacement.Offset.fail();
        int range = com.simibubi.create.infrastructure.config.AllConfigs.server().equipment.placementAssistRange.get();
        final var reach = player == null ? null : player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_INTERACTION_RANGE);
        if (reach != null && reach.hasModifier(com.simibubi.create.content.equipment.extendoGrip.ExtendoGripItem.singleRangeAttributeModifier.id())) range += 4;
        for (final var side : AssistedPlacement.Helper.orderedByDistance(pos, hit.getLocation(), direction -> direction.getAxis() == axis)) {
            BlockPos target = pos.relative(side);
            int poles = 0;
            while (poles < range && LoadedLevelAccess.isLoaded(level, target)) {
                final var next = LoadedLevelAccess.blockState(level, target);
                if (!this.extendsPole.test(next) || !next.hasProperty(this.property) || !this.value.apply(next).equals(propertyValue)) break;
                poles++; target = target.relative(side);
            }
            if (poles < range && LoadedLevelAccess.isLoaded(level, target) && LoadedLevelAccess.blockState(level, target).canBeReplaced())
                return AssistedPlacement.Offset.success(target, placed -> placed.setValue(this.property, propertyValue));
        }
        return AssistedPlacement.Offset.fail();
    }
}
