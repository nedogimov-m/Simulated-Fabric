package dev.simulated_team.simulated.fabric.client.gui;

import net.createmod.catnip.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/** Native click/narration/tooltip owner for the typewriter's custom widgets. */
public abstract class NativePanelWidget extends AbstractWidget {
    public static final Color HEADER_RGB = new Color(0x5391e1, false);
    public static final Color HINT_RGB = new Color(0x96b7e0, false);
    protected float z;
    protected boolean wasHovered;
    protected final List<Component> toolTip = new ArrayList<>();
    protected BiConsumer<Integer, Integer> onClick = (x, y) -> {};
    public int lockedTooltipX = -1, lockedTooltipY = -1;
    protected NativePanelWidget(int x, int y) { this(x, y, 16, 16); }
    protected NativePanelWidget(int x, int y, int width, int height) { this(x, y, width, height, Component.empty()); }
    protected NativePanelWidget(int x, int y, int width, int height, Component message) { super(x, y, width, height, message); }
    @SuppressWarnings("unchecked") public <T extends NativePanelWidget> T withCallback(BiConsumer<Integer, Integer> callback) { onClick = callback; return (T) this; }
    public <T extends NativePanelWidget> T withCallback(Runnable callback) { return withCallback((x, y) -> callback.run()); }
    @SuppressWarnings("unchecked") public <T extends NativePanelWidget> T atZLevel(float value) { z = value; return (T) this; }
    @SuppressWarnings("unchecked") public <T extends NativePanelWidget> T setActive(boolean value) { active = value; return (T) this; }
    public List<Component> getToolTip() { return toolTip; }
    public void tick() {}
    @Override protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int x, int y, float partial) {
        beforeRender(graphics, x, y, partial);
        try { doRender(graphics, x, y, partial); }
        finally { afterRender(graphics, x, y, partial); }
        renderTooltip(graphics, x, y, partial);
        wasHovered = isHoveredOrFocused();
    }
    protected void beforeRender(GuiGraphicsExtractor graphics, int x, int y, float partial) { graphics.pose().pushMatrix(); }
    protected void doRender(GuiGraphicsExtractor graphics, int x, int y, float partial) {}
    protected void afterRender(GuiGraphicsExtractor graphics, int x, int y, float partial) { graphics.pose().popMatrix(); }
    protected void renderTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        if (!isHovered() || toolTip.isEmpty()) return;
        graphics.setComponentTooltipForNextFrame(Minecraft.getInstance().font, toolTip,
                lockedTooltipX < 0 ? mouseX : lockedTooltipX + getX(), lockedTooltipY < 0 ? mouseY : lockedTooltipY + getY());
    }
    protected boolean clicked(double x, double y) { return isMouseOver(x, y); }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (active && visible && isValidClickButton(event.buttonInfo()) && !clicked(event.x(), event.y())) return false;
        return super.mouseClicked(event, doubleClick);
    }
    public void runCallback(double x, double y) { onClick.accept((int) x, (int) y); }
    @Override public void onClick(MouseButtonEvent event, boolean doubleClick) { runCallback(event.x(), event.y()); }
    @Override protected void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }
    public void setHeight(int value) { height = value; }
}
