package dev.simulated_team.simulated.fabric.client.gui;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.GuiRenderState;

public interface NativeGuiAccess {
    GuiRenderState simulated$guiState();
    ScreenRectangle simulated$scissor();
}
