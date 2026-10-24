package dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen.widgets;

import com.mojang.blaze3d.platform.InputConstants;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen.EntryModifierScreen;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.fabric.client.gui.NativePanelWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class PromptWidget extends NativePanelWidget {

    private final EntryModifierScreen entryModifierScreen;
    protected boolean bindingActive = false;

    public PromptWidget(final EntryModifierScreen entryModifierScreen, final int x, final int y, final int width, final int height) {
        super(x, y, width, height);
        this.entryModifierScreen = entryModifierScreen;
    }

    @Override
    protected void doRender(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        super.doRender(graphics, mouseX, mouseY, partialTicks);

        if (this.entryModifierScreen.modifying && this.entryModifierScreen.psuedoEntry != null) {
            Component displayName = InputConstants.Type.KEYBOARD.getOrCreate(this.entryModifierScreen.psuedoEntry.scanCode).getDisplayName();

            if (this.bindingActive) {
                displayName = SimLang.translate("linked_typewriter.bind_screen_prompt").component();
            } else if (this.entryModifierScreen.psuedoEntry.scanCode == -1) {
                displayName = SimLang.translate("linked_typewriter.bind_new_key").component();
            }

            graphics.pose().translate(3, 4);
            graphics.text(Minecraft.getInstance().font, displayName, this.getX(), this.getY(), 0xFFFFFFFF, true);
        }
    }

    @Override
    public void onClick(final net.minecraft.client.input.MouseButtonEvent event, final boolean doubleClick) {
        super.onClick(event, doubleClick);
        this.bindingActive ^= true;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        final int keyCode = event.key(), scanCode = event.keycode(), modifiers = event.modifiers();
        if (this.bindingActive && this.entryModifierScreen.psuedoEntry != null) {
            this.entryModifierScreen.psuedoEntry.keyCode(keyCode);
            this.bindingActive = false;
            return true;
        }

        this.bindingActive = false;
        return super.keyPressed(event);
    }
}
