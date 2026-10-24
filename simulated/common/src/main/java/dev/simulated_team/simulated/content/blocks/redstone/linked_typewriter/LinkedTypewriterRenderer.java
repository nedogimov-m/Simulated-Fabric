package dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import java.util.ArrayList;
import java.util.List;

public final class LinkedTypewriterRenderer extends NativePartsRenderer<LinkedTypewriterBlockEntity> {
    private static final List<LerpedFloat> keys = new ArrayList<>(14);
    static { for (int i = 0; i < 14; i++) keys.add(LerpedFloat.linear().startWithValue(0)); }
    public LinkedTypewriterRenderer(BlockEntityRendererProvider.Context context) {}

    public static void tick() {
        if (Minecraft.getInstance().isPaused() || LinkedTypewriterInteractionHandler.getMode() == LinkedTypewriterInteractionHandler.Mode.IDLE) return;
        for (int i = 0; i < keys.size(); i++) {
            final var key = keys.get(i);
            key.chase(LinkedTypewriterInteractionHandler.getPressedKeys().contains(i) ? 1 : 0, .4f, LerpedFloat.Chaser.EXP);
            key.tickChaser();
        }
    }
    public static void resetKeys() { keys.forEach(key -> key.startWithValue(0)); }
    @Override protected void extractParts(LinkedTypewriterBlockEntity entity, float partial, Parts parts) {
        extractKeys(entity, partial, parts, new PoseStack(), parts.light());
    }
    /** Shared world/GUI extraction; callbacks receive only copied mesh/pose/light values. */
    public static void extractKeys(LinkedTypewriterBlockEntity entity, float partial, Parts parts, PoseStack pose, int light) {
        final float s = 1 / 16f;
        final var player = Minecraft.getInstance().player;
        final boolean animate = player != null && entity.checkUser(player.getUUID());
        if (LinkedTypewriterInteractionHandler.getMode() == LinkedTypewriterInteractionHandler.Mode.BIND) {
            final int pulse = (int) Mth.lerp((Mth.sin(AnimationTickHolder.getRenderTime() / 4f) + 1) / 2, 5f, 15f);
            light = pulse << 20;
        }
        pose.pushPose();
        pose.translate(.5, 4 * s, .5);
        pose.rotate(Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(entity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING))));
        pose.translate(-7 * s, s, 2 * s);
        pose.pushPose();
        int index = 0;
        for (int i = 0; i < 6; i++) { pose.translate(2 * s, 0, 0); key(parts, pose, light, partial, animate, index++, false); }
        pose.popPose();
        pose.translate(-s, -s, 2 * s);
        pose.pushPose();
        for (int i = 0; i < 7; i++) { pose.translate(2 * s, 0, 0); key(parts, pose, light, partial, animate, index++, false); }
        pose.popPose();
        pose.translate(8 * s, -s, 2 * s);
        key(parts, pose, light, partial, animate, index, true);
        pose.popPose();
    }
    private static void key(Parts parts, PoseStack pose, int light, float partial, boolean animate, int index, boolean space) {
        pose.pushPose();
        pose.translate(0, animate ? -.75f / 16 * keys.get(index).getValue(partial) : 0, 0);
        parts.add(space ? SimPartialModels.LINKED_TYPEWRITER_KEY_SPACEBAR : SimPartialModels.LINKED_TYPEWRITER_KEY,
                pose, -1, null, false, light);
        pose.popPose();
    }
}
