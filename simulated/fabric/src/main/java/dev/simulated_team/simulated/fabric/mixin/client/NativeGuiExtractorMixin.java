package dev.simulated_team.simulated.fabric.mixin.client;

import dev.simulated_team.simulated.fabric.client.gui.NativeGuiAccess;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Read-only access to the same native GUI frame/scissor state used by vanilla blits. */
@Mixin(GuiGraphicsExtractor.class)
abstract class NativeGuiExtractorMixin implements NativeGuiAccess {
    @Shadow @Final private GuiRenderState guiRenderState;
    @Shadow @Final private GuiGraphicsExtractor.ScissorStack scissorStack;
    @Override public GuiRenderState simulated$guiState() { return guiRenderState; }
    @Override public ScreenRectangle simulated$scissor() { return scissorStack.peek(); }
}
