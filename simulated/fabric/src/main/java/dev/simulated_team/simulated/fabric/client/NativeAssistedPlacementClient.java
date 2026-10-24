package dev.simulated_team.simulated.fabric.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.mixin.client.BlockItemPlacementPreviewInvoker;
import com.simibubi.create.foundation.model.PartialModelRenderState;
import com.simibubi.create.foundation.utility.LoadedLevelAccess;
import dev.simulated_team.simulated.api.AssistedPlacement;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Read-only native placement prediction; render submission consumes an immutable mesh snapshot. */
public final class NativeAssistedPlacementClient {
    public record Prediction(BlockPos source, BlockPos target, BlockState state, InteractionHand hand) {
        public Prediction { source = source.immutable(); target = target.immutable(); }
    }
    private record Frame(Prediction placement, Mesh mesh) {
        void submit(final SubmitNodeCollector collector, final Vec3 camera) {
            final var pose = new PoseStack();
            pose.translate(this.placement.target.getX() - camera.x, this.placement.target.getY() - camera.y, this.placement.target.getZ() - camera.z);
            pose.translate(.5, .5, .5); pose.scale(.85f, .85f, .85f); pose.translate(-.5, -.5, -.5);
            final var state = new PartialModelRenderState(); state.mesh = this.mesh; state.translucent = true;
            state.submit(pose, collector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        }
    }
    private static final Map<LevelRenderState, Frame> FRAMES = Collections.synchronizedMap(new WeakHashMap<>());
    private static boolean registered;
    private static int animationTicks;
    public static @Nullable Prediction predict(final Minecraft client) {
        final var level = client.level; final var player = client.player;
        if (level == null || player == null || client.gui.screen() != null || client.gui.hud.isHidden()
                || player.isShiftKeyDown() || player.isSpectator() || !player.mayBuild()
                || !(client.hitResult instanceof final BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
                || !LoadedLevelAccess.isLoaded(level, hit.getBlockPos()) || !level.mayInteract(player, hit.getBlockPos())) return null;
        final var clicked = LoadedLevelAccess.blockState(level, hit.getBlockPos());
        for (final var hand : InteractionHand.values()) {
            if (hand == InteractionHand.OFF_HAND && !player.getMainHandItem().isEmpty()) return null;
            final var stack = player.getItemInHand(hand);
            if (stack.isEmpty() || !(stack.getItem() instanceof final BlockItem item)) continue;
            for (final var helper : AssistedPlacement.helpers()) {
                if (!helper.matchesItem(stack) || !helper.matchesState(clicked)) continue;
                final var offset = helper.getOffset(player, level, clicked, hit.getBlockPos(), hit, stack);
                if (!offset.successful()) continue;
                final var target = offset.target();
                if (!level.isInValidBounds(target) || !level.getWorldBorder().isWithinBounds(target)
                        || !level.mayInteract(player, target) || !player.mayUseItemAt(target, hit.getDirection(), stack)
                        || !item.getBlock().isEnabled(level.enabledFeatures())) return null;
                for (int x = -2; x <= 2; x += 2) for (int z = -2; z <= 2; z += 2)
                    if (!LoadedLevelAccess.isLoaded(level, target.offset(x, 0, z))) return null;
                var context = AssistedPlacement.context(new UseOnContext(player, hand, hit), offset);
                if (!context.getClickedPos().equals(target) || !context.canPlace()) return null;
                context = item.updatePlacementContext(context);
                if (context == null || !context.getClickedPos().equals(target)) return null;
                var state = ((BlockItemPlacementPreviewInvoker) item).create$kineticPreviewPlacementState(context);
                if (state == null) return null;
                state = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).apply(state);
                return new Prediction(hit.getBlockPos(), target, state, hand);
            }
        }
        return null;
    }
    private static @Nullable Frame extract(final Minecraft client) {
        final var prediction = predict(client);
        if (prediction == null || animationTicks == 0) return null;
        final var model = new PartialModelRenderState(); AllPartialModels.extract(prediction.state, model);
        if (model.mesh == null || model.mesh.size() == 0) return null;
        final float breathe = .55f - .2f * (float) Math.cos(2 * Math.PI * (System.currentTimeMillis() % 2500) / 2500d);
        final int tint = Math.clamp(Math.round(breathe * .75f * (animationTicks / 10f) * 255), 0, 255) << 24 | 0xffffff;
        final var mutable = Renderer.get().mutableMesh(); final var emitter = mutable.emitter();
        model.mesh.forEach(quad -> {
            emitter.copyFrom(quad).chunkLayer(ChunkSectionLayer.TRANSLUCENT).cullFace(null).emissive(true).tintIndex(-1);
            for (int vertex = 0; vertex < 4; vertex++) emitter.color(vertex, ARGB.multiply(quad.color(vertex), tint));
            emitter.emit();
        });
        return new Frame(prediction, mutable.immutableCopy());
    }
    private static void clear() { FRAMES.clear(); animationTicks = 0; }
    public static void register() {
        if (registered) return; registered = true;
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, level) -> clear());
        ClientTickEvents.END_CLIENT_TICK.register(client -> animationTicks = predict(client) == null
                ? Math.max(0, animationTicks - 2) : Math.min(10, animationTicks + 1));
        LevelExtractionEvents.END_EXTRACTION.register(context -> {
            final var frame = extract(Minecraft.getInstance());
            if (frame == null) FRAMES.remove(context.levelState()); else FRAMES.put(context.levelState(), frame);
        });
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            final var frame = FRAMES.get(context.levelState());
            if (frame != null) frame.submit(context.submitNodeCollector(), context.levelState().cameraRenderState.pos);
        });
    }
    private NativeAssistedPlacementClient() { }
}
