package dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen.widgets;

import dev.simulated_team.simulated.fabric.client.gui.NativeIconButton;
import com.simibubi.create.foundation.gui.AllIcons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

public class ConfirmationWidgetBase extends NativeIconButton {

    public boolean confirmation;
    public MutableComponent message;

    public ConfirmationWidgetBase(final int x, final int y, final AllIcons icon) {
        super(x, y, icon);
    }

    public <T extends ConfirmationWidgetBase> T withMessage(final MutableComponent component) {
        this.message = component;
        return (T) this;
    }

    @Override
    public void doRender(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        super.doRender(graphics, mouseX, mouseY, partialTicks);

        if (this.isHovered && this.visible && this.active && this.confirmation) {
            this.renderHoveredText(graphics, mouseX, mouseY);
        }
    }

    public void renderHoveredText(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        graphics.setComponentTooltipForNextFrame(Minecraft.getInstance().font, List.of(this.message.withColor(0xff0000)), mouseX, mouseY);
    }

    @Override
    protected boolean clicked(final double mouseX, final double mouseY) {
        if (!this.isMouseOver(mouseX, mouseY)) {
            this.confirmation = false;
        }

        return super.clicked(mouseX, mouseY);
    }

    @Override
    public void onClick(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        final double mouseX = event.x(), mouseY = event.y();
        if (this.confirmation) {
            this.runCallback(mouseX, mouseY);
            this.confirmation = false;
        } else {
            this.confirmation = true;
        }
    }
}
