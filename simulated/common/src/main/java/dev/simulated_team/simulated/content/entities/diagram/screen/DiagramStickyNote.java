package dev.simulated_team.simulated.content.entities.diagram.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.simulated_team.simulated.content.entities.diagram.DiagramConfig;
import dev.simulated_team.simulated.index.SimGUITextures;
import dev.simulated_team.simulated.util.SimpleSubLevelGroupRenderer;
import foundry.veil.api.client.render.gpu.GpuFramebuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.joml.*;

import java.lang.Math;

public class DiagramStickyNote extends DiagramButton {

    private static final SimGUITextures NOTE_TEXTURE = SimGUITextures.DIAGRAM_STICKY_NOTE;

    private static final int SUBLEVEL_RENDER_WIDTH_PIXELS = 88;
    private static final int SUBLEVEL_RENDER_HEIGHT_PIXELS = 88;

    private static final int SUBLEVEL_RENDER_X_OFFSET = 8;
    private static final int SUBLEVEL_RENDER_Y_OFFSET = 7;

    public static final int MAX_OFFSET = NOTE_TEXTURE.width;
    public static final int MIN_OFFSET = 9;

    private final Vector3d NOTE_LOCAL_CAM_POS = new Vector3d();
    private final Vector3d NOTE_CAMERA_POS = new Vector3d();
    private final Matrix4f NOTE_PROJ_MAT = new Matrix4f();

    private final Quaternionf NOTE_ORIENTATION = new Quaternionf();
    private DiagramScreen parent;

    private float lastOffset = MIN_OFFSET;
    private float currentOffset = MIN_OFFSET;

    private GpuFramebuffer fbo;
    private GpuFramebuffer outlineFbo;
    private GpuFramebuffer finalFbo;

    private float renderTime = 0;
    private final int renderXStart;

    public DiagramStickyNote(final DiagramScreen parent, final int diagramX, final int diagramY, final Component message, final Runnable onClick) {
        super(NOTE_TEXTURE, 0, diagramY + 5, message, onClick);

        this.renderXStart = (diagramX + SimGUITextures.DIAGRAM.width) - NOTE_TEXTURE.width + MIN_OFFSET;
        this.setX(this.renderXStart);

        this.parent = parent;
    }


    public void tick() {
        this.lastOffset = this.currentOffset;

        float target = MIN_OFFSET;
        if (this.active) {
            target = MAX_OFFSET - 8;
        }

        this.currentOffset = Mth.lerp(DiagramScreen.PAPER_SLIDE_SPEED, this.currentOffset, target);
        this.setX((int) (this.renderXStart + this.currentOffset));
    }

    private float lerpedOffset(final float pt) {
        return Mth.lerp(pt, this.lastOffset, this.currentOffset);
    }

    public void activate() {
        if (!this.active) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
            this.active = true;
        }
    }

    public void deactivate() {
        this.active = false;
    }

    public void create(final DiagramConfig.NoteConfigs noteConfigs) {
        closeTargets();
        final var device=RenderSystem.getDevice();
        try {
            this.fbo=GpuFramebuffer.builder("Simulated note scene",SUBLEVEL_RENDER_WIDTH_PIXELS,SUBLEVEL_RENDER_HEIGHT_PIXELS).color(GpuFormat.RGBA8_UNORM).depth(GpuFormat.D32_FLOAT).build(device);
            this.outlineFbo=GpuFramebuffer.builder("Simulated note outline",SUBLEVEL_RENDER_WIDTH_PIXELS,SUBLEVEL_RENDER_HEIGHT_PIXELS).color(GpuFormat.RGBA8_UNORM).build(device);
            this.finalFbo=GpuFramebuffer.builder("Simulated note final",SUBLEVEL_RENDER_WIDTH_PIXELS,SUBLEVEL_RENDER_HEIGHT_PIXELS).color(GpuFormat.RGBA8_UNORM).build(device);
        } catch(Throwable failure) {closeTargets();throw failure;}

        this.active = noteConfigs.isActive();
        this.updateOrientation();

        if (this.active) {
            this.currentOffset = MAX_OFFSET - 8;
            this.lastOffset = this.currentOffset;
        }

        this.visible = true;
    }

    public void free() {
        this.deactivate();

        NOTE_ORIENTATION.identity();
        closeTargets();
        this.visible=false;

        this.parent = null;
    }

    private void closeTargets() {
        if(this.fbo!=null){this.fbo.close();this.fbo=null;}
        if(this.outlineFbo!=null){this.outlineFbo.close();this.outlineFbo=null;}
        if(this.finalFbo!=null){this.finalFbo.close();this.finalFbo=null;}
    }

    public void updateCurrentScope(final Vector2dc start, final Vector2dc end, final Vector3dc localPosition, final Matrix4fc projMatrix) {
        this.updateOrientation();

        final int width = DiagramScreen.DIAGRAM_TEXTURE.width;
        final int height = DiagramScreen.DIAGRAM_TEXTURE.height;
        final Vector3d startPlotSpace = DiagramScreen.getPlotCoords(start, NOTE_ORIENTATION, localPosition, projMatrix, width, height);
        final Vector3d endPlotSpace = DiagramScreen.getPlotCoords(end, NOTE_ORIENTATION, localPosition, projMatrix, width, height);

        this.parent.config.getNoteConfigs().getNoteScope().set(startPlotSpace.x, startPlotSpace.y, startPlotSpace.z, endPlotSpace.x, endPlotSpace.y, endPlotSpace.z);
    }

    public void handleInternalUpdate(final Vector2d magnifyingTarget, final Vector2d inverseTarget) {
        magnifyingTarget.sub(this.getSublevelRenderX(), this.getSublevelRenderY());
        inverseTarget.sub(this.getSublevelRenderX(), this.getSublevelRenderY());

        final int width = SUBLEVEL_RENDER_WIDTH_PIXELS;
        final int height = SUBLEVEL_RENDER_HEIGHT_PIXELS;
        final Vector3d startPlotSpace = DiagramScreen.getPlotCoords(magnifyingTarget, NOTE_ORIENTATION, NOTE_LOCAL_CAM_POS, NOTE_PROJ_MAT, width, height);
        final Vector3d endPlotSpace = DiagramScreen.getPlotCoords(inverseTarget, NOTE_ORIENTATION, NOTE_LOCAL_CAM_POS, NOTE_PROJ_MAT, width, height);

        this.parent.config.getNoteConfigs().getNoteScope().set(startPlotSpace.x, startPlotSpace.y, startPlotSpace.z, endPlotSpace.x, endPlotSpace.y, endPlotSpace.z);
    }

    private void updateOrientation() {
        this.renderTime = 100;
        NOTE_ORIENTATION.identity().rotateY((float) Math.toRadians(this.parent.config.getNoteConfigs().getNoteYaw())).rotateX((float) Math.toRadians(this.parent.config.getNoteConfigs().getNotePitch()));
    }

    public boolean contains(double x, double y) {
        if (!this.active) {
            return false;
        }

        x -= this.getSublevelRenderX();
        y -= this.getSublevelRenderY();
        return x > 0 && x < SUBLEVEL_RENDER_WIDTH_PIXELS && y > 0  && y < SUBLEVEL_RENDER_HEIGHT_PIXELS;
    }

    public Vector2d clamp(final Vector2d dest) {
        final float minX = this.getSublevelRenderX();
        final float minY = this.getSublevelRenderY();
        dest.max(new Vector2d(minX, minY));
        dest.min(new Vector2d(minX + SUBLEVEL_RENDER_WIDTH_PIXELS, minY + SUBLEVEL_RENDER_HEIGHT_PIXELS));
        return dest;
    }

    private float getSublevelRenderX() {
        return this.renderXStart + this.currentOffset + SUBLEVEL_RENDER_X_OFFSET;
    }

    private float getSublevelRenderY() {
        return this.getY() + SUBLEVEL_RENDER_Y_OFFSET;
    }

    @Override
    protected void extractWidgetRenderState(final GuiGraphicsExtractor guiGraphics, final int mouseX, final int mouseY, final float partialTicks) {
        final var ps = guiGraphics.pose();
        ps.pushMatrix();

        final float currentX = this.renderXStart + this.lerpedOffset(partialTicks);
        final int currentY = this.getY();
        ps.translate(currentX, currentY);
        SimGUITextures.DIAGRAM_STICKY_NOTE.render(guiGraphics, 0, 0);

        if (this.active) {
            ps.pushMatrix();
            ps.translate(SUBLEVEL_RENDER_X_OFFSET, SUBLEVEL_RENDER_Y_OFFSET);
            if (!SimpleSubLevelGroupRenderer.isRendering() && this.fbo != null) {
                this.populateFBO(partialTicks);
                DiagramScreen.renderFBO(guiGraphics, this.finalFbo, SUBLEVEL_RENDER_WIDTH_PIXELS, SUBLEVEL_RENDER_HEIGHT_PIXELS);
            }

            this.parent.renderArrows(guiGraphics,
                    mouseX,
                    mouseY,
                    (int) currentX + SUBLEVEL_RENDER_X_OFFSET,
                    currentY + SUBLEVEL_RENDER_Y_OFFSET,
                    NOTE_ORIENTATION,
                    NOTE_LOCAL_CAM_POS,
                    NOTE_PROJ_MAT,
                    SUBLEVEL_RENDER_WIDTH_PIXELS,
                    SUBLEVEL_RENDER_HEIGHT_PIXELS);

            this.renderCustomCOM(guiGraphics, ps);
            ps.popMatrix();

        }

        ps.popMatrix();

    }

    public void populateFBO(final float partialTicks) {
        if (this.renderTime >= 20.0f / DiagramScreen.FPS) {
            this.renderTime = 0.0f;
        } else {
            this.renderTime += Minecraft.getInstance().getDeltaTracker().getRealtimeDeltaTicks();
            return;
        }

        final float zNear = 0.1f;
        final LevelPlot plot = this.parent.subLevel.getPlot();
        final dev.ryanhcode.sable.companion.math.BoundingBox3ic plotBounds = plot.getBoundingBox();
        final float maxDistance = Math.max(Math.max(plotBounds.maxX() - plotBounds.minX(), plotBounds.maxY() - plotBounds.minY()), plotBounds.maxZ() - plotBounds.minZ()) + 1;

        final BoundingBox3ic scopeBounds = new BoundingBox3i(this.parent.config.getNoteConfigs().getNoteScope());
        float radius = Math.max(Math.max(scopeBounds.maxX() - scopeBounds.minX(), scopeBounds.maxY() - scopeBounds.minY()), scopeBounds.maxZ() - scopeBounds.minZ()) + 1;
        radius *= 0.55F;

        radius = Math.max(radius, 1.0f);

        final Vector3d plotBoundsCenter = new Vector3d((scopeBounds.minX() + scopeBounds.maxX() + 1) / 2.0, (scopeBounds.minY() + scopeBounds.maxY() + 1) / 2.0, (scopeBounds.minZ() + scopeBounds.maxZ() + 1) / 2.0);
        final float aspect = (float) SUBLEVEL_RENDER_WIDTH_PIXELS / SUBLEVEL_RENDER_HEIGHT_PIXELS;
        NOTE_PROJ_MAT.identity().ortho(-radius * aspect, radius * aspect, -radius, radius, zNear, maxDistance * 2.0f);

        // account for the smaller screen size
        NOTE_LOCAL_CAM_POS.set(plotBoundsCenter.add(NOTE_ORIENTATION.transform(new Vector3d(0, 0, maxDistance))));

        final Pose3dc renderPose = this.parent.subLevel.renderPose(partialTicks);
        renderPose.transformPosition(NOTE_CAMERA_POS.set(NOTE_LOCAL_CAM_POS));

        DiagramScreen.draw(this.parent.subLevel, partialTicks, NOTE_ORIENTATION, NOTE_PROJ_MAT, NOTE_CAMERA_POS, SUBLEVEL_RENDER_WIDTH_PIXELS, SUBLEVEL_RENDER_HEIGHT_PIXELS, this.fbo, this.outlineFbo, this.finalFbo, 0.75f, 1.15f, 0x6e684d, 0x59543e);
    }

    @Override
    public void playDownSound(final SoundManager handler) {

    }

    private void renderCustomCOM(final GuiGraphicsExtractor guiGraphics, final Matrix3x2fStack stack) {
        if (this.parent.config.displayCenterOfMass()) {
            stack.pushMatrix();
            final Vector3d centerOfMass = new Vector3d(this.parent.subLevel.logicalPose().rotationPoint());
            final Vector2d screenCoords = DiagramScreen.getScreenCoords(centerOfMass, NOTE_ORIENTATION, NOTE_LOCAL_CAM_POS, NOTE_PROJ_MAT, SUBLEVEL_RENDER_WIDTH_PIXELS, SUBLEVEL_RENDER_HEIGHT_PIXELS);

            SimGUITextures tex = SimGUITextures.DIAGRAM_ICON_COM_TINY;
            final double comOffsetX = (screenCoords.x) - 8;
            final double comOffsetY = (screenCoords.y) - 8;

            if (comOffsetY > 0 && comOffsetX > 0 && comOffsetY < SUBLEVEL_RENDER_HEIGHT_PIXELS && comOffsetX < SUBLEVEL_RENDER_WIDTH_PIXELS) {
                stack.translate((float)comOffsetX, (float)comOffsetY);
                tex.render(guiGraphics,0,0);
            } else {
                final float centerX = SUBLEVEL_RENDER_WIDTH_PIXELS / 2f;
                final float centerY = SUBLEVEL_RENDER_HEIGHT_PIXELS / 2f;

                final Vector2d target = new Vector2d(screenCoords.x() - centerX, screenCoords.y - centerY).normalize();
                stack.translate(centerX,centerY).rotate((float)Math.atan2(target.x,-target.y)).translate(-8,-8).translate(0,-40);

                tex = SimGUITextures.DIAGRAM_ICON_COM_ARROW;
                tex.render(guiGraphics,0,0);
            }
            stack.popMatrix();
        }
    }
}
