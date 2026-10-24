package dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen.widgets;

import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterEntries;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen.LinkedTypewriterScreen;
import dev.simulated_team.simulated.index.SimGUITextures;
import net.createmod.catnip.data.Couple;
import dev.simulated_team.simulated.index.SimIcons;
import net.minecraft.client.renderer.RenderPipelines;
import dev.simulated_team.simulated.fabric.client.gui.NativePanelWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.NotNull;


public class KeyWidget extends NativePanelWidget {

    private final LinkedTypewriterEntries.KeyboardEntry EMPTY;

    public int keyNum;

    public boolean bound = false;
    public boolean keyboardActive;

    private final SimIcons icon;

    private final LinkedTypewriterScreen screen;

    public KeyWidget(final int pX, final int pY, final int pWidth, final int key, final SimIcons keyIcon, final LinkedTypewriterScreen screen) {
        super(pX, pY, pWidth, 14, Component.empty());
        this.keyNum = key;
        this.EMPTY = new LinkedTypewriterEntries.KeyboardEntry(RedstoneLinkNetworkHandler.Frequency.EMPTY, RedstoneLinkNetworkHandler.Frequency.EMPTY, key, BlockPos.ZERO);
        this.icon = keyIcon;

        this.screen = screen;
    }

    public void render(final GuiGraphicsExtractor pGuiGraphics, final int x, final int y, final int pMouseX, final int pMouseY, final float pPartialTick, final boolean keyboardActive) {
        this.bound = this.screen.getNewEntries().getKeyMap().containsKey(this.keyNum);

        this.setX(x);
        this.setY(y);
        this.keyboardActive = keyboardActive;
        this.extractRenderState(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    public void extractWidgetRenderState(@NotNull final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        final SimGUITextures start = this.bound ? SimGUITextures.KEY_START : SimGUITextures.INACTIVE_KEY_START;
        final SimGUITextures middle = this.bound ? SimGUITextures.KEY_MIDDLE : SimGUITextures.INACTIVE_KEY_MIDDLE;
        final SimGUITextures end = this.bound ? SimGUITextures.KEY_END : SimGUITextures.INACTIVE_KEY_END;

        final int midWidth = this.width - start.width - end.width;
        final int endX = start.width + midWidth;
        final boolean mouseHover = this.isMouseOver(mouseX, mouseY);
        final int y = this.getY() + (mouseHover && this.keyboardActive ? 2 : 0);

        start.render(graphics, this.getX(), y);

        for (int i = 0; i < midWidth / 2; i++) {
            middle.render(graphics, this.getX() + start.width + i * 2, y);
        }

        end.render(graphics, this.getX() + endX, y);

        if (this.icon != null) {
            this.icon.render(graphics, this.getX() + 3, y + 4,
                    this.bound ? 0xff724731 : 0xff512018);
        }

        if (this.isHovered) {
            this.renderHover(graphics, mouseX, mouseY, partialTicks);
        }
    }

    protected void renderHover(@NotNull final GuiGraphicsExtractor pGuiGraphics, final int pMouseX, final int pMouseY, final float pPartialTick) {
        LinkedTypewriterEntries.KeyboardEntry keyboardEntry = this.screen.getNewEntries().getEntry(this.keyNum);
        if (keyboardEntry == null) {
            keyboardEntry = this.EMPTY;
        }

        if (this.keyboardActive) {
            final SimGUITextures arrow = SimGUITextures.LINKED_TYPEWRITER_TOOLTIP_ARROW;
            final SimGUITextures freq = SimGUITextures.LINKED_TYPEWRITER_FREQUENCY;

            final Component keyName = this.keyName();
            final int textLength = Minecraft.getInstance().font.width(keyName.getString());
            final int textHeight = Minecraft.getInstance().font.lineHeight;

            final int freqWidth = 20;
            final int freqHeight = 20;

            final int minWidth = arrow.width + 4 + freqWidth;
            final int bgWidth = Math.max(minWidth, textLength + 8);
            final int bgHeight = textHeight + 12 + freqHeight;
            final int yOffset = 8;

            final int textX = (this.width / 2) - (textLength / 2) + 1;
            final int bgX = (this.width / 2) - (bgWidth / 2);
            final int arrowX = (this.width / 2) - (arrow.width / 2);
            final int freqX = (this.width / 2) - (freq.width / 2);

            this.extractBackground(pGuiGraphics, this.getX() + bgX, this.getY() - bgHeight - yOffset, bgWidth, bgHeight);
            arrow.render(pGuiGraphics, this.getX() + arrowX, this.getY() - yOffset - 2);

            freq.render(pGuiGraphics, this.getX() + freqX, this.getY() - yOffset - bgHeight + 4);

            final Couple<RedstoneLinkNetworkHandler.Frequency> coupled = keyboardEntry.getAsCouple();
            pGuiGraphics.item(coupled.getFirst().getStack(), this.getX() + freqX + 1, this.getY() - yOffset - bgHeight + 5);
            pGuiGraphics.item(coupled.getSecond().getStack(), this.getX() + freqX + 19, this.getY() - yOffset - bgHeight + 5);

            pGuiGraphics.text(Minecraft.getInstance().font, keyName.getString(), this.getX() + textX - 1, this.getY() - yOffset - textHeight - 2, 0xff000000 | DyeColor.BLACK.getTextColor(), false);
        }
    }

    private Component keyName() {
        return InputConstants.Type.KEYBOARD.getOrCreate(this.keyNum)
                .getDisplayName();
    }

    private void extractBackground(@NotNull final GuiGraphicsExtractor pGuiGraphics, final int x, final int y, final int w, final int h) {
        final SimGUITextures bg = SimGUITextures.LINKED_TYPEWRITER_TOOLTIP_BACKGROUND;
        pGuiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, bg.location, x, y, w, h);
    }
}