package dev.simulated_team.simulated.content.display_sources;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.NumericSingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import dev.simulated_team.simulated.content.blocks.lasers.laser_sensor.LaserSensorBlockEntity;
import dev.simulated_team.simulated.data.SimLang;
import net.minecraft.network.chat.MutableComponent;

public class LaserSensorDisplaySource extends NumericSingleLineDisplaySource {
    @Override
    protected MutableComponent provideLine(final DisplayLinkContext context, final DisplayTargetStats displayTargetStats) {
        if (!(context.getSourceBlockEntity() instanceof final LaserSensorBlockEntity be)) {
            return EMPTY.getFirst();
        }

        return (be.closestHitDistance == Double.MAX_VALUE) ?
                EMPTY.getFirst() :
                SimLang.number(be.closestHitDistance)
                        .space()
                        .text("block" + (be.closestHitDistance != 1 ? "s" : ""))
                        .component();
    }

    @Override
    public java.util.List<com.simibubi.create.content.redstone.displayLink.DisplaySourceConfiguration> configuration(final DisplayLinkContext context) {
        var options = new java.util.ArrayList<>(super.configuration(context));
        options.add(new com.simibubi.create.content.redstone.displayLink.DisplaySourceConfiguration(
                "LaserSensorSelection", java.util.List.of("simulated.display_source.laser_sensor.laser_distance"), 0,
                0, 0, 1, 0, 1, 0, 88, "", ""));
        return java.util.List.copyOf(options);
    }

    @Override
    protected String getTranslationKey() {
        return "laser_sensor.data";
    }

    @Override
    protected boolean allowsLabeling(final DisplayLinkContext displayLinkContext) {
        return true;
    }
}
