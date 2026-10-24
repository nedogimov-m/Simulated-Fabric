package dev.simulated_team.simulated.fabric.client.gui;

import com.simibubi.create.content.trains.schedule.ScheduleTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import dev.simulated_team.simulated.index.SimIcons;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

/** Original atlas button with native event dispatch and inherited narration/tooltips. */
public class NativeIconButton extends NativePanelWidget {
    private AllIcons icon;
    private boolean pressed;
    public boolean green;
    public NativeIconButton(int x, int y, AllIcons icon) { super(x, y, 18, 18); this.icon = icon; }
    public void setIcon(AllIcons icon) { this.icon = icon; }
    public void setToolTip(Component text) { toolTip.clear(); toolTip.add(text); setMessage(text); }
    @Override public void onClick(MouseButtonEvent event, boolean doubleClick) { pressed = true; super.onClick(event, doubleClick); }
    @Override public void onRelease(MouseButtonEvent event) { pressed = false; }
    @Override public boolean keyPressed(KeyEvent event) {
        if (!active || !visible || !event.isConfirmation()) return false;
        playDownSound(net.minecraft.client.Minecraft.getInstance().getSoundManager());
        runCallback(getX() + width / 2d, getY() + height / 2d);
        return true;
    }
    @Override protected void doRender(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partial) {
        final var background = !active ? ScheduleTextures.BUTTON_DISABLED : isHoveredOrFocused()
                ? pressed ? ScheduleTextures.BUTTON_DOWN : ScheduleTextures.BUTTON_HOVER
                : green ? ScheduleTextures.BUTTON_GREEN : ScheduleTextures.BUTTON;
        background.render(gui, getX(), getY());
        renderIcon(icon, gui, getX() + 1, getY() + 1, -1);
    }
    public static void renderIcon(AllIcons icon, GuiGraphicsExtractor gui, int x, int y, int color) {
        if (icon instanceof SimIcons custom) custom.render(gui, x, y, color);
        else gui.blit(RenderPipelines.GUI_TEXTURED, AllIcons.ICON_ATLAS,
                x, y, icon.getIconX(), icon.getIconY(), 16, 16, AllIcons.ICON_ATLAS_SIZE, AllIcons.ICON_ATLAS_SIZE, color);
    }
}
