package dev.simulated_team.simulated.index;


import com.simibubi.create.foundation.gui.AllIcons;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public class SimIcons extends AllIcons implements net.createmod.catnip.gui.element.ScreenElement {
    public static final Identifier ICON_ATLAS = Identifier.parse("simulated:textures/gui/icons.png");
    public static final int ICON_ATLAS_SIZE = 64;

    private static int x = 0, y = -1;
    private final int iconX;
    private final int iconY;

    public static final SimIcons
            //Docking connector
            HALF_EXTEND = newRow(),
            FULL_EXTEND = next(),

            //linked typewriter
            ADD_OR_EDIT = newRow(),
            HAMBURGER = next(),
            CANCEL = next(),
            CONFIG = next(),

            //small arrows
            KEY_ARROW_UP = newRow(),
            KEY_ARROW_LEFT = next(),
            KEY_ARROW_DOWN = next(),
            KEY_ARROW_RIGHT = next();

    public SimIcons(final int x, final int y) {
        super(x, y);
        this.iconX = x * 16;
        this.iconY = y * 16;
    }

    private static SimIcons next() {
        return new SimIcons(++x, y);
    }

    private static SimIcons newRow() {
        return new SimIcons(x = 0, ++y);
    }

    @Override
    public void render(final GuiGraphicsExtractor graphics, final int x, final int y) {
        render(graphics, x, y, -1);
    }

    public void render(final GuiGraphicsExtractor graphics, final int x, final int y, final int color) {
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, ICON_ATLAS,
                x, y, this.iconX, this.iconY, 16, 16, ICON_ATLAS_SIZE, ICON_ATLAS_SIZE, color);
    }
}
