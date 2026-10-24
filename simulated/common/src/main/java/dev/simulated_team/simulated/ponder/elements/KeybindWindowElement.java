package dev.simulated_team.simulated.ponder.elements;

import net.createmod.catnip.gui.element.ScreenElement;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.InputElementBuilder;
import net.createmod.ponder.enums.PonderGuiTextures;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;

/**
 * Window element that renders a key mapping.
 *
 * @author Ocelot
 */
public class KeybindWindowElement extends net.createmod.ponder.foundation.element.AnimatedOverlayElementBase implements net.createmod.ponder.api.element.PonderOverlayElement {

    private final Vec3 sceneSpace;
    private final Pointing direction;
    @Nullable
    Component keybind;
    @Nullable
    ScreenElement icon;
    ItemStack item = ItemStack.EMPTY;

    public KeybindWindowElement(final Vec3 sceneSpace, final Pointing direction) {
        this.sceneSpace = sceneSpace;
        this.direction = direction;
    }

    public @NotNull Builder builder() {
        return new KeybindWindowElement.Builder();
    }

    public class Builder implements InputElementBuilder {

        @Override
        public Builder withItem(final ItemStack stack) {
            KeybindWindowElement.this.item = stack;
            return this;
        }

        @Override
        public Builder leftClick() {
            KeybindWindowElement.this.icon = PonderGuiTextures.ICON_LMB;
            return this;
        }

        @Override
        public Builder scroll() {
            KeybindWindowElement.this.icon = PonderGuiTextures.ICON_SCROLL;
            return this;
        }

        @Override
        public Builder rightClick() {
            KeybindWindowElement.this.icon = PonderGuiTextures.ICON_RMB;
            return this;
        }

        @Override
        public Builder showing(final ScreenElement icon) {
            KeybindWindowElement.this.icon = icon;
            return this;
        }

        @Override
        public Builder whileSneaking() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Builder whileCTRL() {
            throw new UnsupportedOperationException();
        }

        public Builder keybind(final String keybind) {
            KeybindWindowElement.this.keybind = Component.keybind(keybind).append(" +");
            return this;
        }
    }

    @Override
    public net.createmod.ponder.api.element.PonderOverlayElement.RenderState extractOverlayRenderState(
            final PonderScene scene, final float partial) {
        final float fade = getFade(partial);
        if (fade < 1 / 16f) return null;
        final var point = scene.getTransform().sceneToScreen(this.sceneSpace, partial);
        final Pointing direction = this.direction;
        final float dx = (direction == Pointing.RIGHT ? -1 : direction == Pointing.LEFT ? 1 : 0) * 10 * (1 - fade);
        final float dy = (direction == Pointing.DOWN ? -1 : direction == Pointing.UP ? 1 : 0) * 10 * (1 - fade);
        final float x = point.x + dx, y = point.y + dy;
        final Component text = this.keybind == null ? Component.empty() : this.keybind.copy();
        final boolean hasText = this.keybind != null;
        final ScreenElement icon = this.icon;
        final ItemStack item = this.item.copy();
        final int color = Math.clamp((int) (fade * 255), 0, 255) << 24 | PonderPalette.WHITE.getColor() & 0xffffff;
        return (phase, gui, font, viewport) -> {
            if (phase != net.createmod.ponder.api.element.PonderOverlayElement.Phase.AFTER_BUILTINS) return;
            final boolean hasItem = !item.isEmpty(), hasIcon = icon != null;
            final int textWidth = hasText ? font.width(text) : 0;
            final int width = textWidth + (hasIcon ? 24 : 0) + (hasItem ? 24 : 0);
            final int height = hasIcon || hasItem ? 24 : 0;
            final var pose = gui.pose();
            pose.pushMatrix();
            try {
                pose.translate(viewport.x() + x, viewport.y() + y);
                PonderUI.renderSpeechBox(gui, 0, 0, width, height, false, direction, true);
                if (hasText) gui.text(font, text, 2, (height - font.lineHeight) / 2 + 2, color, false);
                if (hasIcon) {
                    pose.pushMatrix(); pose.translate(textWidth, 0); pose.scale(1.5f, 1.5f);
                    icon.render(gui, 0, 0); pose.popMatrix();
                }
                if (hasItem) {
                    pose.pushMatrix(); pose.translate(textWidth + (hasIcon ? 24 : 0), 0); pose.scale(1.5f, 1.5f);
                    gui.item(item, 0, 0); pose.popMatrix();
                }
            } finally { pose.popMatrix(); }
        };
    }

}
