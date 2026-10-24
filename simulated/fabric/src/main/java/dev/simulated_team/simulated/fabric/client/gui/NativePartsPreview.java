package dev.simulated_team.simulated.fabric.client.gui;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

/** Native GUI preview of already extracted immutable meshes; never retains a block entity or level. */
public final class NativePartsPreview extends PictureInPictureRenderer<NativePartsPreview.State> {
    public record State(NativePartsRenderer.State parts, int x0, int y0, int x1, int y1,
                        float scale, Matrix3x2fc pose, ScreenRectangle scissorArea,
                        ScreenRectangle bounds) implements PictureInPictureRenderState {
        public State { pose = new Matrix3x2f(pose); }
    }
    public static void extract(GuiGraphicsExtractor gui, NativePartsRenderer.Parts parts, int x, int y, int width, int height) {
        final var access = (NativeGuiAccess) gui;
        final var pose = new Matrix3x2f(gui.pose());
        final var scissor = access.simulated$scissor();
        final var bounds = PictureInPictureRenderState.getBounds(x, y, x + width, y + height, pose, scissor);
        if (bounds == null) return;
        access.simulated$guiState().addPicturesInPictureState(new State(NativePartsRenderer.State.from(parts),
                x, y, x + width, y + height, 1, pose, scissor, bounds));
    }
    @Override public Class<State> getRenderStateClass() { return State.class; }
    @Override protected String getTextureLabel() { return "Simulated native parts preview"; }
    @Override protected void renderToTexture(State state, PoseStack pose, SubmitNodeCollector collector) {
        Minecraft.getInstance().gameRenderer.lighting().setupFor(Lighting.Entry.ITEMS_3D);
        pose.translate(-(state.x1() - state.x0()) / 2f, -(state.y1() - state.y0()), 0);
        // Restore GUI pixel coordinates from native PIP's bottom-center origin and Z reflection.
        pose.scale(1, 1, -1);
        NativePartsRenderer.submitParts(state.parts(), pose, collector);
    }
    public static void register() { PictureInPictureRendererRegistry.register(context -> new NativePartsPreview()); }
}
