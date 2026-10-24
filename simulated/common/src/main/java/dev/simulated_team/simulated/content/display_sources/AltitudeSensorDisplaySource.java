package dev.simulated_team.simulated.content.display_sources;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.NumericSingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import dev.simulated_team.simulated.content.blocks.altitude_sensor.AltitudeSensorBlockEntity;
import dev.simulated_team.simulated.data.SimLang;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class AltitudeSensorDisplaySource extends NumericSingleLineDisplaySource {

    @Override
    protected MutableComponent provideLine(final DisplayLinkContext context, final DisplayTargetStats stats) {
        if (!(context.getSourceBlockEntity() instanceof final AltitudeSensorBlockEntity be)) {
            return ZERO.copy();
        }

        switch (context.sourceConfig().getIntOr("AltitudeSensorSelection", 0)) {
            case 0 -> {
                assert be.hasLevel();
                final float airPressure = (float) be.getAirPressure() * 100.0f;
                return Component.literal(String.format("%.2f%%", airPressure));
            }
            case 1 -> {
                return Component.literal(String.format("%.2f", be.getWorldHeight()));
            }
        }
        return EMPTY_LINE.copy();
    }

    @Override
    public java.util.List<com.simibubi.create.content.redstone.displayLink.DisplaySourceConfiguration> configuration(final DisplayLinkContext context) {
        var options = new java.util.ArrayList<>(super.configuration(context));
        options.add(new com.simibubi.create.content.redstone.displayLink.DisplaySourceConfiguration(
                "AltitudeSensorSelection", java.util.List.of("simulated.display_source.altitude_sensor.air_pressure", "simulated.display_source.altitude_sensor.height"), 0,
                0, 0, 1, 0, 1, 0, 95, "", ""));
        return java.util.List.copyOf(options);
    }

    @Override
    protected String getTranslationKey() {
        return "altitude_sensor.data";
    }

    @Override
    protected boolean allowsLabeling(final DisplayLinkContext context) {
        return true;
    }
}
