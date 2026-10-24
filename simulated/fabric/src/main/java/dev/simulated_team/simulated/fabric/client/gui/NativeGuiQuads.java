package dev.simulated_team.simulated.fabric.client.gui;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

/** Detached GUI geometry, submitted to vanilla's batcher with its actual nested scissor. */
public final class NativeGuiQuads {
    public record TexturedQuad(TextureSetup textureSetup, Matrix3x2fc pose,
            float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1,
            int topLeft, int bottomLeft, int bottomRight, int topRight, ScreenRectangle scissorArea) implements GuiElementRenderState {
        public TexturedQuad { pose = new Matrix3x2f(pose); }
        @Override public RenderPipeline pipeline() { return RenderPipelines.GUI_TEXTURED; }
        @Override public ScreenRectangle bounds() {
            final int left = (int) Math.floor(Math.min(x0, x1)), top = (int) Math.floor(Math.min(y0, y1));
            final var transformed = new ScreenRectangle(left, top,
                    (int) Math.ceil(Math.max(x0, x1)) - left, (int) Math.ceil(Math.max(y0, y1)) - top).transformMaxBounds(pose);
            return scissorArea == null ? transformed : transformed.intersection(scissorArea);
        }
        @Override public void buildVertices(VertexConsumer out) {
            out.addVertexWith2DPose(pose, x0, y0).setUv(u0, v0).setColor(topLeft);
            out.addVertexWith2DPose(pose, x0, y1).setUv(u0, v1).setColor(bottomLeft);
            out.addVertexWith2DPose(pose, x1, y1).setUv(u1, v1).setColor(bottomRight);
            out.addVertexWith2DPose(pose, x1, y0).setUv(u1, v0).setColor(topRight);
        }
    }
    public static void texturedGradient(GuiGraphicsExtractor gui, Identifier texture,
            float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1,
            int topColor, int bottomColor) {
        submit(gui, texture, x0, y0, x1, y1, u0, v0, u1, v1, topColor, bottomColor, bottomColor, topColor);
    }
    public static void texturedHorizontalGradient(GuiGraphicsExtractor gui, Identifier texture,
            float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1,
            int leftColor, int rightColor) {
        submit(gui, texture, x0, y0, x1, y1, u0, v0, u1, v1, leftColor, leftColor, rightColor, rightColor);
    }
    private static void submit(GuiGraphicsExtractor gui, Identifier texture,
            float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1,
            int topLeft, int bottomLeft, int bottomRight, int topRight) {
        final var image = Minecraft.getInstance().getTextureManager().getTexture(texture);
        final var access = (NativeGuiAccess) gui;
        access.simulated$guiState().addGuiElement(new TexturedQuad(
                TextureSetup.singleTexture(image.getTextureView(), image.getSampler()), gui.pose(),
                x0, y0, x1, y1, u0, v0, u1, v1, topLeft, bottomLeft, bottomRight, topRight, access.simulated$scissor()));
    }
    private NativeGuiQuads() {}
}
