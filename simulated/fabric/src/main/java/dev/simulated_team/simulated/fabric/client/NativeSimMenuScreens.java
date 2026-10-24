package dev.simulated_team.simulated.fabric.client;

import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen.LinkedTypewriterScreen;
import dev.simulated_team.simulated.index.SimMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;

public final class NativeSimMenuScreens {
    public static void register() {
        dev.simulated_team.simulated.fabric.client.gui.NativePartsPreview.register();
        MenuScreens.register(SimMenuTypes.LINKED_TYPEWRITER, LinkedTypewriterScreen::new); }
    private NativeSimMenuScreens() {}
}
