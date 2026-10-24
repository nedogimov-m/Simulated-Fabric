package dev.eriksonn.aeronautics.content.display_sources;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.NumericSingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.content.redstone.displayLink.DisplaySourceConfiguration;
import java.util.List;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.ServerBalloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.BlockEntityLiftingGasProvider;
import dev.eriksonn.aeronautics.data.AeroLang;
import net.createmod.catnip.lang.LangNumberFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;

public class GasDisplaySource extends NumericSingleLineDisplaySource {

    @Override
    protected MutableComponent provideLine(final DisplayLinkContext displayLinkContext, final DisplayTargetStats displayTargetStats) {
        if (!(displayLinkContext.getSourceBlockEntity() instanceof final BlockEntityLiftingGasProvider provider)) {
            return ZERO.copy();
        }

        if (!(provider.getBalloon() instanceof final ServerBalloon info)) {
            return noBalloon();
        }

        switch (displayLinkContext.sourceConfig().getIntOr("GasDataSelection", 0)) {
            case 0 -> { // volume
                final int totalBar = 15;
                final int capacity = info.getCapacity();
                final int targetBar = (int) Math.ceil(totalBar * info.getTotalTargetVolume() / capacity);
                final int volume = Mth.clamp((int) Math.ceil(totalBar * (info.getTotalFilledVolume() + info.getTotalVolumeChange()) / capacity), 0, totalBar);

                return barComponent(volume, targetBar, totalBar);
            }
            case (1) -> { // total lift
                return AeroLang.text(LangNumberFormat.format(info.getTotalLift())).component();
            }
        }

        return ZERO.copy();
    }

    private static MutableComponent noBalloon() {
        return AeroLang.text("No Balloon above").component();
    }

    static MutableComponent barComponent(final int amount, final int target, final int total) {
        final int lower = Math.min(amount, target - 1);
        final int upper = Math.max(amount - target, 0);
        final char filledChar = '█';
        final char halfFillChar = '▒';
        final char emptyChar = '░';
        return Component.empty()
                .append(bars(Math.max(0, lower), ChatFormatting.DARK_AQUA, filledChar))
                .append(bars(Math.max(0, target - lower - 1), ChatFormatting.DARK_GRAY, halfFillChar))/*
                .append(bars(upper, ChatFormatting.DARK_AQUA, filledChar))
                .append(bars(Math.max(0, total - target - upper), ChatFormatting.DARK_GRAY, filledChar))*/;

    }

    private static MutableComponent bars(final int count, final ChatFormatting format, final char ch) {
        return Component.literal(String.valueOf(ch).repeat(count))
                .withStyle(format);
    }

    @Override
    public List<DisplaySourceConfiguration> configuration(final DisplayLinkContext context) {
        final var options = new java.util.ArrayList<>(super.configuration(context));
        options.add(new DisplaySourceConfiguration("GasDataSelection",
                List.of("aeronautics.display_source.lifting_gas.volume", "aeronautics.display_source.lifting_gas.total_lift"),
                0, 0, 0, 1, 0, 1, 0, 60, "", ""));
        return List.copyOf(options);
    }

    @Override
    protected boolean allowsLabeling(final DisplayLinkContext displayLinkContext) {
        return true;
    }

    @Override
    protected String getTranslationKey() {
        return "lifting_gas.data";
    }
}
