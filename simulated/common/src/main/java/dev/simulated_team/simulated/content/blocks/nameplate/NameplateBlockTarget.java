package dev.simulated_team.simulated.content.blocks.nameplate;

import com.simibubi.create.api.behaviour.display.DisplayTarget;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import dev.simulated_team.simulated.api.ConditionalDisplayTarget;
import dev.simulated_team.simulated.data.SimLang;
import net.createmod.catnip.theme.Color;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

public class NameplateBlockTarget extends ConditionalDisplayTarget {
    public static final NameplateBlockTarget NAMEPLATE = Registry.register(CreateBuiltInRegistries.DISPLAY_TARGET,
            Identifier.fromNamespaceAndPath("simulated", "nameplate"), new NameplateBlockTarget());

    @Override
    public boolean allowsWriting(final DisplayLinkContext context) {
        return context.getTargetBlockEntity() instanceof final NameplateBlockEntity nbe && nbe.waxed;
    }

    @Override
    public Component getErrorMessage(final DisplayLinkContext context) {
        return SimLang.translate("nameplate.target.unwaxed").color(Color.RED).component();
    }

    @Override
    public void acceptText(final int line, final List<MutableComponent> text, final DisplayLinkContext context) {
        if (context.getTargetBlockEntity() instanceof final NameplateBlockEntity nbe && nbe.waxed) {
            nbe.setName(text.get(0).getString(), true, null);
        }
    }

    @Override
    public DisplayTargetStats provideStats(final DisplayLinkContext context) {
        return new DisplayTargetStats(1, 0, this);
    }

    @Override
    public Component getLineOptionText(final int line) {
        return CreateLang.translateDirect("display_target.single_line");
    }
}
