package dev.simulated_team.simulated.index;

import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen.LinkedTypewriterMenuCommon;
import dev.simulated_team.simulated.fabric.menu.TypewriterOpeningData;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/** Native common menu registry. Screens are bound in the client initializer. */
public final class SimMenuTypes {
    public static final ExtendedMenuType<LinkedTypewriterMenuCommon, TypewriterOpeningData> LINKED_TYPEWRITER = Registry.register(
            BuiltInRegistries.MENU, Identifier.parse("simulated:linked_typewriter"),
            new ExtendedMenuType<>(LinkedTypewriterMenuCommon::fromOpening, TypewriterOpeningData.CODEC));
    public static void register() {}
    private SimMenuTypes() {}
}
