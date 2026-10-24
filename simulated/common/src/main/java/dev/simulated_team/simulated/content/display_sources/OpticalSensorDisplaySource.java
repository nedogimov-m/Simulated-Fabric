package dev.simulated_team.simulated.content.display_sources;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.NumericSingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import dev.simulated_team.simulated.content.blocks.lasers.optical_sensor.OpticalSensorBlockEntity;
import dev.simulated_team.simulated.data.SimLang;
import net.minecraft.network.chat.MutableComponent;

public class OpticalSensorDisplaySource extends NumericSingleLineDisplaySource {

    @Override
    protected MutableComponent provideLine(final DisplayLinkContext context, final DisplayTargetStats stats) {
        if (!(context.getSourceBlockEntity() instanceof final OpticalSensorBlockEntity be)) {
            return ZERO.copy();
        }

        switch (context.sourceConfig().getIntOr("OpticalSensorSelection", 0)) {
            case 0 -> {
                return be.hasHit() ? be.getHitBlock().getName() : SimLang.text("No Block Detected").component();
            }
            case 1 -> {
                if (!be.hasHit()) {
                    return SimLang.text("No Block Detected").component();
                }
                final float rayDistance = be.getRayDistance();
                return SimLang.number(rayDistance)
                        .space()
                        .text("block" + (rayDistance != 1 ? "s" : ""))
                        .component();
            }
        }

        return ZERO.copy();
    }

    @Override
    public java.util.List<com.simibubi.create.content.redstone.displayLink.DisplaySourceConfiguration> configuration(final DisplayLinkContext context) {
        var options = new java.util.ArrayList<>(super.configuration(context));
        options.add(new com.simibubi.create.content.redstone.displayLink.DisplaySourceConfiguration(
                "OpticalSensorSelection", java.util.List.of("simulated.display_source.optical_sensor.detected_block", "simulated.display_source.optical_sensor.block_distance"), 0,
                0, 0, 1, 0, 1, 0, 85, "", ""));
        return java.util.List.copyOf(options);
    }

    @Override
    protected String getTranslationKey() {
        return "optical_sensor.data";
    }

    @Override
    protected boolean allowsLabeling(final DisplayLinkContext context) {
        return true;
    }
}
