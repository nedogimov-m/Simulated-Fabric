package dev.simulated_team.simulated.fabric.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import java.util.Collection;

/** Original Catnip panel layout/lifecycle on native Minecraft screen extraction. See bundled MIT license. */
public abstract class NativePanelScreen extends Screen {
    protected int windowWidth, windowHeight, windowXOffset, windowYOffset, guiLeft, guiTop;
    protected NativePanelScreen(Component title) { super(title); }
    protected NativePanelScreen() { this(Component.empty()); }
    protected void setWindowSize(int width, int height) { windowWidth = width; windowHeight = height; }
    protected void setWindowOffset(int x, int y) { windowXOffset = x; windowYOffset = y; }
    @Override protected void init() {
        guiLeft = (width - windowWidth) / 2 + windowXOffset;
        guiTop = (height - windowHeight) / 2 + windowYOffset;
    }
    @Override public void tick() {
        for (final var child : children()) {
            if (child instanceof NativePanelWidget widget) widget.tick();
            else if (child instanceof com.simibubi.create.foundation.gui.widget.ScrollInput input) input.tick();
        }
    }
    @Override public boolean isPauseScreen() { return false; }
    @SafeVarargs protected final <W extends GuiEventListener & Renderable & NarratableEntry> void addRenderableWidgets(W... widgets) {
        for (final var widget : widgets) addRenderableWidget(widget);
    }
    protected <W extends GuiEventListener & Renderable & NarratableEntry> void addRenderableWidgets(Collection<W> widgets) {
        widgets.forEach(this::addRenderableWidget);
    }
    protected void removeWidgets(GuiEventListener... widgets) { for (final var widget : widgets) removeWidget(widget); }
    protected void removeWidgets(Collection<? extends GuiEventListener> widgets) { widgets.forEach(this::removeWidget); }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        partial = net.createmod.catnip.animation.AnimationTickHolder.getPartialTicksUI();
        graphics.pose().pushMatrix();
        try {
            prepareFrame();
            renderWindowBackground(graphics, mouseX, mouseY, partial);
            renderWindow(graphics, mouseX, mouseY, partial);
            super.extractRenderState(graphics, mouseX, mouseY, partial);
            renderWindowForeground(graphics, mouseX, mouseY, partial);
            endFrame();
        } finally { graphics.pose().popMatrix(); }
    }
    @Override public boolean keyPressed(KeyEvent event) {
        final boolean consumed = super.keyPressed(event);
        if (consumed || getFocused() != null) return consumed;
        if (minecraft.options.keyInventory.matches(event)) { onClose(); return true; }
        boolean handled = false;
        for (final var child : children())
            if (child instanceof NativePanelWidget widget) handled |= widget.keyPressed(event);
        return handled;
    }
    @Override public GuiEventListener getFocused() {
        final var focused = super.getFocused();
        if (focused instanceof net.minecraft.client.gui.components.AbstractWidget widget && !widget.isFocused()) {
            setFocused(null);
            return null;
        }
        return focused;
    }
    protected void prepareFrame() {}
    // Screen.extractRenderStateWithTooltipAndSubtitles already extracts the background.
    // This hook is only for a panel-specific layer, such as the diagram's tint.
    protected void renderWindowBackground(GuiGraphicsExtractor graphics, int x, int y, float partial) {}
    protected abstract void renderWindow(GuiGraphicsExtractor graphics, int x, int y, float partial);
    protected void renderWindowForeground(GuiGraphicsExtractor graphics, int x, int y, float partial) {}
    protected void endFrame() {}
}
