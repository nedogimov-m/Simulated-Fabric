package dev.simulated_team.simulated.content.display_sources;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.SingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;
import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.NavigationTarget;
import dev.simulated_team.simulated.data.SimLang;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.time.Duration;

public class NavigationTableDisplaySource extends SingleLineDisplaySource {
	@Override
	protected MutableComponent provideLine(final DisplayLinkContext context, final DisplayTargetStats stats) {
		if (!(context.getSourceBlockEntity() instanceof final NavTableBlockEntity be)) {
			return EMPTY_LINE.copy();
		}

		switch (context.sourceConfig().getIntOr("NavTableSelection", 0)) {
			case 0 -> {
				final NavigationTarget navigationTarget = be.getNavTableItem();
				if(navigationTarget == null) return EMPTY_LINE.copy();

				final int distance = (int) navigationTarget.distanceToTarget(be);
				return Component.literal(String.valueOf(distance));
			}
			case 1 -> {
				final double distance = be.distanceToTarget();
				final double lastDistance = be.lastDistanceToTarget();
				final double change = lastDistance - distance;
				final double speed = change / .5f;

				final int totalSeconds = (int) (distance / speed);
				final Duration duration = Duration.ofSeconds(totalSeconds);

				String eta = "%2s:%2s".formatted(duration.toMinutesPart(), duration.toSecondsPart());

				if(duration.toHoursPart() > 0) {
					eta = "%2s:".formatted(duration.toHoursPart()) + eta;
				}

				if(totalSeconds < 0 || change < 0.001) {
					return Component.literal("N/A");
				}

				return Component.literal(eta.replace(' ', '0'));
			}
		}
		return EMPTY_LINE.copy();
	}

    @Override
    public java.util.List<com.simibubi.create.content.redstone.displayLink.DisplaySourceConfiguration> configuration(final DisplayLinkContext context) {
        var options = new java.util.ArrayList<>(super.configuration(context));
        options.add(new com.simibubi.create.content.redstone.displayLink.DisplaySourceConfiguration(
                "NavTableSelection", java.util.List.of("simulated.display_source.navigation_table.distance", "simulated.display_source.navigation_table.eta_real"), 0,
                0, 0, 1, 0, 1, 0, 95, "", ""));
        return java.util.List.copyOf(options);
    }

	@Override
	protected String getTranslationKey() {
		return "navigation_table.data";
	}

	@Override
	protected boolean allowsLabeling(final DisplayLinkContext context) {
		return true;
	}
}
