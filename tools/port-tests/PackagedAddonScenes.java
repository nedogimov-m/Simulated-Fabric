import com.mojang.blaze3d.platform.InputConstants;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterBlockEntity;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen.LinkedTypewriterScreen;
import dev.simulated_team.simulated.content.end_sea.EndSeaPhysicsData;
import dev.simulated_team.simulated.content.end_sea.EndSeaShadowRenderer;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.network.packets.linked_typewriter.TypewriterKeySavePacket;
import dev.simulated_team.simulated.service.SimMenuService;
import dev.simulated_team.simulated.util.SimpleSubLevelGroupRenderer;
import foundry.veil.api.event.VeilRenderLevelStageEvent;
import foundry.veil.platform.VeilEventPlatform;
import io.netty.buffer.Unpooled;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Production-only scene probes, called inside the suite's existing integrated world; no replacement classes. */
public final class PackagedAddonScenes {
    private record ReturnPoint(Identifier dimension, double x, double y, double z, float yaw, float pitch, GameType mode) { }
    private record Placement(BlockPos pos, BlockState previous) { }

    public static void run(ClientGameTestContext test, TestSingleplayerContext world, List<String> checks) {
        var server = world.getServer();
        ReturnPoint origin = server.computeOnServer(instance -> {
            var player = player(instance);
            return new ReturnPoint(player.level().dimension().identifier(), player.getX(), player.getY(), player.getZ(),
                    player.getYRot(), player.getXRot(), player.gameMode.getGameModeForPlayer());
        });
        if (!Boolean.getBoolean("port.smoke.ponderOnly")) {
        var active = new AtomicBoolean(true);
        var endFrames = new AtomicInteger();
        test.runOnClient(client -> VeilEventPlatform.INSTANCE.onVeilRenderLevelStageFinal((stage, context) -> {
            if (!active.get() || stage != VeilRenderLevelStageEvent.Stage.END_MAIN || client.level == null
                    || client.player == null || EndSeaShadowRenderer.renderingShadowMap() || SimpleSubLevelGroupRenderer.isRendering()
                    || !client.level.dimension().equals(Level.END)
                    || client.player.getY() < -40 || client.player.getY() > -30) return;
            var physics = EndSeaPhysicsData.of(client.level);
            var shadows = EndSeaShadowRenderer.getShadowsFramebuffer();
            if (physics != null && shadows != null && shadows.hasDepth()
                    && !shadows.color(0).view().isClosed() && !shadows.depth().view().isClosed()) endFrames.incrementAndGet();
        }));
        try {
            server.runCommand("gamemode spectator @a");
            server.runCommand("execute in minecraft:the_end run tp @a 0 -35 0");
            test.waitFor(client -> client.level != null && client.level.dimension().equals(Level.END)
                    && client.player != null && Math.abs(client.player.getY() + 35) < .1 && endFrames.get() >= 3, 600);
            test.runOnClient(client -> {
                var physics = EndSeaPhysicsData.of(client.level);
                check(physics != null && physics.dimension().equals(Level.END.identifier()) && physics.startY() == -40,
                        "Original packaged End Sea physics resource was not active");
                check(EndSeaShadowRenderer.getShadowsFramebuffer() != null, "Actual End Sea shadow pass did not allocate its framebuffer");
            });
            var endScreenshot = test.takeScreenshot("suite-end-sea");
            checks.add("End Sea: original the_end physics resource, spectator at 0,-35,0, at least three actual END_MAIN frames with live shadow color/depth; screenshot=" + endScreenshot);
        } finally {
            active.set(false);
            teleportBack(world, origin);
        }
        test.waitFor(client -> client.level != null && client.level.dimension().identifier().equals(origin.dimension())
                && client.player != null && client.player.distanceToSqr(origin.x(), origin.y(), origin.z()) < 4, 400);

        // The production service calls real ServerPlayer.openMenu(ExtendedMenuProvider),
        // supplies TypewriterOpeningData and delegates menu construction to the actual BE.
        server.runCommand("gamemode creative @a");
        Placement placed = server.computeOnServer(instance -> {
            var player = player(instance); var level = player.level();
            BlockPos pos = player.blockPosition().offset(2, 0, 0);
            var previous = level.getBlockState(pos);
            check(level.setBlock(pos, SimBlocks.LINKED_TYPEWRITER.defaultBlockState(), 3), "Native server did not place the registered typewriter block");
            check(level.getBlockEntity(pos) instanceof LinkedTypewriterBlockEntity, "Registered typewriter BE factory was not used");
            return new Placement(pos, previous);
        });
        try {
            test.waitFor(client -> client.level != null && client.level.getBlockEntity(placed.pos()) instanceof LinkedTypewriterBlockEntity, 200);
            server.runOnServer(instance -> {
                var player = player(instance);
                var typewriter = (LinkedTypewriterBlockEntity) player.level().getBlockEntity(placed.pos());
                check(typewriter.checkAndStartUsing(player.getUUID()), "Actual typewriter could not start its user session");
                SimMenuService.INSTANCE.openScreen(player, typewriter);
                check(player.containerMenu != player.inventoryMenu, "Native ServerPlayer.openMenu did not open the production menu");
            });
            test.waitForScreen(LinkedTypewriterScreen.class);
            var typewriter = test.computeOnClient(client -> (LinkedTypewriterScreen) client.gui.screen());
            var typewriterFrames = observeScreen(test, typewriter);
            test.waitFor(client -> client.gui.screen() == typewriter && typewriterFrames.get() >= 2, 200);
            test.runOnClient(client -> {
                check(typewriter.clientBe == client.level.getBlockEntity(placed.pos()), "Native extended opening data did not resolve the real client BE");
                typewriter.modifier.startModifying(null, entry -> {
                    if (entry != null) typewriter.getNewEntries().setKey(entry.getScanCode(), entry);
                });
                var prompt = typewriter.modifier.promptWidget;
                typewriter.setFocused(prompt);
                prompt.onClick(new MouseButtonEvent(prompt.getX() + 2, prompt.getY() + 2,
                        new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)), false);
            });
            // Fabric's actual input driver enters native KeyboardHandler and routes
            // its SDL physical KeyEvent to the focused production PromptWidget.
            test.getInput().pressKey(InputConstants.KEY_Q);
            test.runOnClient(client -> {
                check(typewriter.modifier.psuedoEntry != null && typewriter.modifier.psuedoEntry.scanCode == InputConstants.KEY_Q,
                        "Native keyboard dispatch did not bind physical SDL Q");
                typewriter.getMenu().setGhostSample(0, new ItemStack(Items.DIAMOND));
                typewriter.getMenu().setGhostSample(1, new ItemStack(Items.REDSTONE));
                typewriter.modifier.psuedoEntry.finishModifications();
                var wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), client.level.registryAccess());
                try {
                    var original = new TypewriterKeySavePacket(typewriter.getNewEntries(), placed.pos(), false);
                    TypewriterKeySavePacket.CODEC.encode(wire, original);
                    var decoded = TypewriterKeySavePacket.CODEC.decode(wire);
                    check(!wire.isReadable() && decoded.pos().equals(placed.pos()) && !decoded.clearAll(), "Actual typewriter save packet codec changed its envelope");
                    var entry = decoded.changedKeys().get(InputConstants.KEY_Q);
                    check(entry != null && entry.getScanCode() == InputConstants.KEY_Q && entry.getFirstAsItemStack().is(Items.DIAMOND)
                            && entry.getSecondAsItemStack().is(Items.REDSTONE), "Registry-aware typewriter codec changed physical Q/frequencies");
                } finally { wire.release(); }
            });
            int before = typewriterFrames.get();
            test.waitFor(client -> client.gui.screen() == typewriter && typewriterFrames.get() >= before + 2, 200);
            var typewriterScreenshot = test.takeScreenshot("suite-typewriter");
            test.runOnClient(client -> typewriter.onClose());
            test.waitFor(client -> client.gui.screen() == null, 200);
            server.waitFor(instance -> {
                var be = player(instance).level().getBlockEntity(placed.pos());
                if (!(be instanceof LinkedTypewriterBlockEntity typewriterBe)) return false;
                var entry = typewriterBe.getTypewriterEntries().getEntry(InputConstants.KEY_Q);
                return entry != null && entry.getScanCode() == InputConstants.KEY_Q
                        && entry.getFirstAsItemStack().is(Items.DIAMOND) && entry.getSecondAsItemStack().is(Items.REDSTONE);
            }, 200);
            checks.add("Typewriter: real server block/BE and extended openMenu, native screen extraction, physical SDL Q binding, registry-aware save codec and accepted client-to-server save on close; screenshot=" + typewriterScreenshot);
        } finally {
            test.runOnClient(client -> { if (client.gui.screen() instanceof LinkedTypewriterScreen screen) screen.onClose(); });
            server.runOnServer(instance -> {
                var player = player(instance); player.closeContainer();
                player.level().setBlock(placed.pos(), placed.previous(), 3);
            });
            server.runCommand("gamemode " + origin.mode().getName() + " @a");
        }

        }
        var ponder = test.computeOnClient(client -> PonderUI.of(SimBlocks.ALTITUDE_SENSOR.asItem().getDefaultInstance()));
        check(ponder.getActiveScene().getId().getNamespace().equals("simulated"), "Ponder compiled a foreign scene instead of the actual Sim registration");
        test.runOnClient(client -> client.setScreenAndShow(ponder));
        try {
            var ponderFrames = observeScreen(test, ponder);
            test.waitFor(client -> client.gui.screen() == ponder && ponder.getActiveScene().getCurrentTime() >= 25
                    && ponderFrames.get() >= 3, 400);
            var ponderScreenshot = test.takeScreenshot("suite-ponder");
            checks.add("Ponder: actual registered simulated:altitude_sensor scene compiled through PonderUI.of, advanced beyond tick25 and extracted at least three frames; screenshot=" + ponderScreenshot);
        } finally {
            test.runOnClient(client -> { if (client.gui.screen() == ponder) ponder.onClose(); });
        }
        test.waitFor(client -> client.gui.screen() == null, 200);
    }

    private static AtomicInteger observeScreen(ClientGameTestContext test, Screen screen) {
        var frames = new AtomicInteger();
        test.runOnClient(client -> ScreenEvents.afterExtract(screen).register((current, graphics, x, y, partial) -> frames.incrementAndGet()));
        return frames;
    }
    private static void teleportBack(TestSingleplayerContext world, ReturnPoint origin) {
        world.getServer().runCommand(String.format(Locale.ROOT, "execute in %s run tp @a %.8f %.8f %.8f %.8f %.8f",
                origin.dimension(), origin.x(), origin.y(), origin.z(), origin.yaw(), origin.pitch()));
        world.getServer().runCommand("gamemode " + origin.mode().getName() + " @a");
    }
    private static ServerPlayer player(MinecraftServer server) {
        check(server.getPlayerList().getPlayers().size() == 1, "Packaged addon scene probe requires the suite's one actual player");
        return server.getPlayerList().getPlayers().getFirst();
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private PackagedAddonScenes() { }
}
