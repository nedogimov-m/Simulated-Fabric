import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.textures.GpuTexture;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintHandle;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.render.vanilla.VanillaChunkedSubLevelRenderData;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.simulated_team.simulated.content.end_sea.EndSeaPhysicsData;
import dev.simulated_team.simulated.content.end_sea.EndSeaShadowRenderer;
import dev.simulated_team.simulated.util.SimpleSubLevelGroupRenderer;
import foundry.veil.api.event.VeilRenderLevelStageEvent;
import foundry.veil.platform.VeilEventPlatform;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** New visible-only world case: real Sable geometry seeds the authored End Sea shadows. */
public final class PackagedEndSeaVisible {
    private record Origin(Identifier dimension, double x, double y, double z, float yaw, float pitch, GameType mode) { }
    private static final class Setup {
        final Map<BlockPos, BlockState> originals = new LinkedHashMap<>();
        ServerSubLevel slab;
        FixedConstraintHandle joint;
    }
    private record Pixels(int shadowDepth, int shadowStrength, int seaColor, String mainFormat,
                          float centerDepth, int centerStrength, float centerSurfaceY,
                          String centerRgb, int blueGreenColor, int maxBlueMinusRed, int maxGreenMinusRed, double cameraY, double slabY) { }

    public static void run(ClientGameTestContext test, TestSingleplayerContext world, List<String> checks) {
        var server = world.getServer();
        Origin origin = server.computeOnServer(instance -> {
            var player = player(instance);
            return new Origin(player.level().dimension().identifier(), player.getX(), player.getY(), player.getZ(),
                    player.getYRot(), player.getXRot(), player.gameMode.getGameModeForPlayer());
        });
        var setup = new Setup();
        var active = new AtomicBoolean(true);
        var frames = new AtomicInteger();
        try {
            server.runCommand("gamemode spectator @a");
            // A vertical view through the platform's small opening separates sea
            // fragments from opaque stone and from the End's black void background.
            server.runCommand("execute in minecraft:the_end run tp @a -0.5 -28 -0.5 0 90");
            test.waitFor(client -> client.level != null && client.level.dimension().equals(Level.END)
                    && client.player != null && Math.abs(client.player.getXRot() - 90) < .1, 400);
            server.runOnServer(instance -> {
                var level = instance.getLevel(Level.END);
                check(level != null && EndSeaPhysicsData.of(level) != null, "Original End Sea physics must be active in the actual End");
                var positions = new ArrayList<BlockPos>();
                // End's valid block storage starts above the sea. Assemble at a valid
                // height, then use the actual physics API to move the completed plot.
                for (int z = -4; z < 4; z++) for (int x = -4; x < 4; x++) {
                    if ((x == -1 || x == 0) && (z == -1 || z == 0)) continue;
                    var pos = new BlockPos(x, 96, z); level.getChunk(pos);
                    setup.originals.put(pos, level.getBlockState(pos));
                    check(level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3), "Could not place real slab source block " + pos);
                    positions.add(pos);
                }
                setup.slab = SubLevelAssemblyHelper.assembleBlocks(level, positions.getFirst(), positions, BoundingBox3i.from(positions));
                check(setup.slab != null && !setup.slab.isRemoved(), "Native Sable assembly did not allocate an actual plot");
                for (var pos : positions) check(level.getBlockState(pos).isAir(), "Assembly did not transfer a real stone block into its plot");
            });
            server.waitFor(instance -> setup.slab != null && !setup.slab.isRemoved()
                    && setup.slab.getSelfMassTracker().getMass() > 0 && RigidBodyHandle.of(setup.slab) != null, 200);
            server.runOnServer(instance -> {
                var level = instance.getLevel(Level.END);
                var container = SubLevelContainer.getContainer(level);
                var point = new Vector3d(-.5, -41, -.5);
                var handle = RigidBodyHandle.of(setup.slab);
                handle.teleport(point, new Quaterniond());
                // The real fixed world joint keeps this visual fixture stable while
                // native plot packets/meshes arrive; no simulation implementation is replaced.
                setup.joint = container.physicsSystem().getPipeline().addConstraint(null, setup.slab,
                        new FixedConstraintConfiguration(point,
                                new Vector3d(setup.slab.getMassTracker().getCenterOfMass()), new Quaterniond()));
                check(setup.joint != null && setup.joint.isValid(), "Actual fixed world/slab joint was not created");
            });
            test.runOnClient(client -> VeilEventPlatform.INSTANCE.onVeilRenderLevelStageFinal((stage, context) -> {
                if (!active.get() || stage != VeilRenderLevelStageEvent.Stage.END_MAIN || client.level == null
                        || !client.level.dimension().equals(Level.END) || EndSeaShadowRenderer.renderingShadowMap()
                        || SimpleSubLevelGroupRenderer.isRendering()) return;
                var slab = clientSlab(client, setup);
                if (slab != null && slab.isFinalized() && slab.renderPose().position().y() < -40
                        && slab.getRenderData() instanceof VanillaChunkedSubLevelRenderData data
                        && !data.extract(context.camera().pos, context.camera().cameraEntityPartialTicks).isEmpty()
                        && EndSeaShadowRenderer.getShadowsFramebuffer() != null) frames.incrementAndGet();
            }));
            test.waitFor(client -> frames.get() >= 3, 600);
            var screenshot = test.takeScreenshot("suite-end-sea-visible");
            Pixels pixels = test.computeOnClient(PackagedEndSeaVisible::pixels);
            System.out.println("Visible End Sea actual frame: " + pixels + "; screenshot=" + screenshot);
            check(pixels.shadowDepth() >= 64 && pixels.shadowStrength() >= 64,
                    "Actual stone plot did not seed End Sea shadow depth/strength: " + pixels);
            check(pixels.seaColor() >= 16, "No visible colored sea through the real platform opening: " + pixels);
            checks.add("NEW visible End Sea: 60 real stone blocks assembled into a Sable plot with fixed world joint, finalized client mesh and three actual END_MAIN frames; nonclear shadow depth="
                    + pixels.shadowDepth() + ", shadow strength=" + pixels.shadowStrength() + ", colored sea pixels in central opening="
                    + pixels.seaColor() + ", main format=" + pixels.mainFormat() + "; screenshot=" + screenshot);
        } finally {
            active.set(false);
            server.runOnServer(instance -> {
                if (setup.joint != null && setup.joint.isValid()) setup.joint.remove();
                var level = instance.getLevel(Level.END);
                if (setup.slab != null && !setup.slab.isRemoved()) {
                    setup.slab.getPlot().destroyAllBlocks();
                    SubLevelContainer.getContainer(level).removeSubLevel(setup.slab, SubLevelRemovalReason.REMOVED);
                }
                setup.originals.forEach((pos, state) -> level.setBlock(pos, state, 3));
            });
            server.runCommand(String.format(Locale.ROOT, "execute in %s run tp @a %.8f %.8f %.8f %.8f %.8f",
                    origin.dimension(), origin.x(), origin.y(), origin.z(), origin.yaw(), origin.pitch()));
            server.runCommand("gamemode " + origin.mode().getName() + " @a");
        }
        test.waitFor(client -> client.level != null && client.level.dimension().identifier().equals(origin.dimension())
                && client.player != null && client.player.distanceToSqr(origin.x(), origin.y(), origin.z()) < 4, 400);
    }

    private static ClientSubLevel clientSlab(Minecraft client, Setup setup) {
        var container = SubLevelContainer.getContainer(client.level);
        return container != null && container.getSubLevel(setup.slab.getUniqueId()) instanceof ClientSubLevel slab ? slab : null;
    }
    private static Pixels pixels(Minecraft client) {
        var shadow = EndSeaShadowRenderer.getShadowsFramebuffer();
        check(shadow != null && shadow.hasDepth(), "Production End Sea shadow target is absent");
        int side = 64, depthCount = 0, strengthCount = 0, colored = 0, blueGreen = 0;
        var depth = read(shadow.depth().texture(), shadow.width() / 2 - side / 2, shadow.height() / 2 - side / 2, side, side, 4);
        var strength = read(shadow.color(0).texture(), shadow.width() / 2 - side / 2, shadow.height() / 2 - side / 2, side, side, 4);
        for (int i = 0; i < side * side; i++) {
            if (depth.getFloat(i * 4) > 0) depthCount++;
            if ((strength.get(i * 4 + 3) & 255) > 0) strengthCount++;
        }
        var main = client.gameRenderer.mainRenderTarget().getColorTexture();
        String format = main.getFormat().name();
        int bytes = format.equals("RGBA16_FLOAT") ? 8 : 4;
        check(bytes == 8 || format.startsWith("RGBA8_") || format.startsWith("BGRA8_"), "Unsupported actual main color format " + format);
        int region = Math.min(32, Math.min(main.getWidth(0), main.getHeight(0)));
        var color = read(main, main.getWidth(0) / 2 - region / 2, main.getHeight(0) / 2 - region / 2, region, region, bytes);
        var mainDepth = client.gameRenderer.mainRenderTarget().getDepthTexture();
        check(mainDepth != null && mainDepth.getFormat().name().equals("D32_FLOAT"), "Expected actual native D32 world depth");
        var worldDepth = read(mainDepth, mainDepth.getWidth(0) / 2 - region / 2,
                mainDepth.getHeight(0) / 2 - region / 2, region, region, 4);
        for (int i = 0; i < region * region; i++) {
            float red, green, blue;
            if (bytes == 8) {
                red = Float.float16ToFloat(color.getShort(i * 8)) * 255;
                green = Float.float16ToFloat(color.getShort(i * 8 + 2)) * 255;
                blue = Float.float16ToFloat(color.getShort(i * 8 + 4)) * 255;
            } else {
                boolean bgra = format.startsWith("BGRA");
                red = color.get(i * 4 + (bgra ? 2 : 0)) & 255;
                green = color.get(i * 4 + 1) & 255;
                blue = color.get(i * 4 + (bgra ? 0 : 2)) & 255;
            }
            if (blue >= red + 8 && green >= red + 4 && blue >= 12) blueGreen++;
            // Native End Sea layers preserve caller depth. Nonblack pixels over
            // clear void depth cannot be opaque stone; exclude the HUD crosshair
            // axes as well. Additive authored layers may legitimately saturate white.
            int x = i % region, y = i / region;
            if (worldDepth.getFloat(i * 4) == 0 && Math.abs(x - region / 2) > 2 && Math.abs(y - region / 2) > 2
                    && Math.max(red, Math.max(green, blue)) >= 12) colored++;
        }
        int center = (side / 2) * side + side / 2;
        float centerDepth = depth.getFloat(center * 4);
        var physics = EndSeaPhysicsData.of(client.level);
        float surfaceY = (float) (physics.startY() - 128 + (1 - centerDepth) * 127.5 + .5);
        int middle = (region / 2) * region + region / 2;
        String centerRgb = bytes == 8 ? "float16:" + Float.float16ToFloat(color.getShort(middle * 8)) + ","
                + Float.float16ToFloat(color.getShort(middle * 8 + 2)) + "," + Float.float16ToFloat(color.getShort(middle * 8 + 4))
                : (color.get(middle * 4) & 255) + "," + (color.get(middle * 4 + 1) & 255) + "," + (color.get(middle * 4 + 2) & 255);
        int maxBlue = Integer.MIN_VALUE, maxGreen = Integer.MIN_VALUE;
        if (bytes == 4) for (int i = 0; i < region * region; i++) {
            int red = color.get(i * 4 + (format.startsWith("BGRA") ? 2 : 0)) & 255;
            maxBlue = Math.max(maxBlue, (color.get(i * 4 + (format.startsWith("BGRA") ? 0 : 2)) & 255) - red);
            maxGreen = Math.max(maxGreen, (color.get(i * 4 + 1) & 255) - red);
        }
        var camera = client.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
        var container = SubLevelContainer.getContainer(client.level);
        double slabY = Double.NaN;
        for (var owner : container.getAllSubLevels()) if (owner instanceof ClientSubLevel slab && !slab.isRemoved()) {
            slabY = slab.renderPose().position().y(); break;
        }
        return new Pixels(depthCount, strengthCount, colored, format, centerDepth, strength.get(center * 4 + 3) & 255,
                surfaceY, centerRgb, blueGreen, maxBlue, maxGreen, camera.pos.y, slabY);
    }
    private static ByteBuffer read(GpuTexture texture, int x, int y, int width, int height, int bytes) {
        var device = RenderSystem.getDevice(); int size = width * height * bytes;
        try (var buffer = device.createBuffer(() -> "packaged visible End Sea pixels", GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_READ, size)) {
            var encoder = device.createCommandEncoder(); encoder.copyTextureToBuffer(texture, buffer, 0, () -> {}, 0, x, y, width, height);
            try (var fence = encoder.createFence()) {
                encoder.submit(); check(fence.awaitCompletion(10_000_000_000L), "Visible End Sea readback timed out");
            }
            try (var mapped = buffer.map(true, false)) {
                byte[] data = new byte[size]; mapped.data().get(data); return ByteBuffer.wrap(data).order(ByteOrder.nativeOrder());
            }
        }
    }
    private static ServerPlayer player(MinecraftServer server) {
        check(server.getPlayerList().getPlayers().size() == 1, "Visible End Sea case requires one actual suite player");
        return server.getPlayerList().getPlayers().getFirst();
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private PackagedEndSeaVisible() { }
}
