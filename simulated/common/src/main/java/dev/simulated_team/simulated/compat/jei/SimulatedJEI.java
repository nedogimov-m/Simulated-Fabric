package dev.simulated_team.simulated.compat.jei;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.client.SearchAlias;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen.LinkedTypewriterScreen;
import dev.simulated_team.simulated.index.SimResourceManagers;
import dev.simulated_team.simulated.fabric.registry.NativeItemCatalog;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IIngredientAliasRegistration;
import mezz.jei.api.registration.IModInfoRegistration;
import net.minecraft.resources.Identifier;

import org.jspecify.annotations.NullMarked;

@JeiPlugin
@SuppressWarnings("unused")
@NullMarked
public class SimulatedJEI implements IModPlugin {

    private static final Identifier ID = Simulated.path("jei_plugin");

    @Override
    public Identifier getPluginUid() {
        return ID;
    }

    @Override
    public void registerGuiHandlers(final IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(LinkedTypewriterScreen.class, new TypewriterGhostIngredientHandler());
        registration.addGuiContainerHandler(LinkedTypewriterScreen.class, new IGuiContainerHandler<>() {
            @Override
            public java.util.List<net.minecraft.client.renderer.Rect2i> getGuiExtraAreas(final LinkedTypewriterScreen screen) {
                return screen.getExtraAreas();
            }
        });
    }

    @Override
    public void registerModInfo(final IModInfoRegistration modAliasRegistration) {
        final var mods = NativeItemCatalog.entries().stream().map(entry -> entry.section().getNamespace())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        for (final String mod : mods) {
            for (String otherMod : mods.stream().filter(v -> !v.equals(mod)).toList()) {
                modAliasRegistration.addModAliases(mod, otherMod);
            }
        }
    }

    @Override
    public void registerIngredientAliases(final IIngredientAliasRegistration registration) {
        for (final SearchAlias searchAlias : SimResourceManagers.SEARCH_ALIAS.entries()) {
            registration.addAliases(VanillaTypes.ITEM_STACK, searchAlias.getItems(), searchAlias.terms());
        }
    }
}
