package dev.simulated_team.simulated.content.display_sources;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.NumericSingleLineDisplaySource;

import java.util.List;

public abstract class AbstractNumericDisplaysource extends NumericSingleLineDisplaySource {

    @Override
    public java.util.List<com.simibubi.create.content.redstone.displayLink.DisplaySourceConfiguration> configuration(final DisplayLinkContext context) {
        var options = new java.util.ArrayList<>(super.configuration(context));
        options.add(new com.simibubi.create.content.redstone.displayLink.DisplaySourceConfiguration(
                this.getSelectionKey(), this.getOptions(), 0,
                0, 0, 1, 0, 1, 0, this.getWidth(), "", ""));
        return java.util.List.copyOf(options);
    }

    public int getWidth() {
        return 100;
    }

    abstract List<String> getOptions();
    abstract String getKey();
    abstract String getSelectionKey();

    @Override
    protected String getTranslationKey() {
        return this.getKey();
    }

    @Override
    protected boolean allowsLabeling(final DisplayLinkContext displayLinkContext) {
        return true;
    }
}
