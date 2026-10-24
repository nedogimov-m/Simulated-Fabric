package dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter;

import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.simulated_team.simulated.content.blocks.redstone.AbstractLinkedReceiverBlockEntity;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.network.packets.linked_typewriter.TypewriterSaveKeyToItemPacket;
import dev.simulated_team.simulated.util.SimColors;
import foundry.veil.api.network.VeilPacketManager;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.lang.FontHelper;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;


import java.util.ArrayList;
import java.util.List;

/**
 * Handles item binding. methods, ticks, etc called from interaction handler.
 */
public class LinkedTypewriterItemBindHandler {

    public static final HudElement OVERLAY = LinkedTypewriterItemBindHandler::renderOverlay;

    private static boolean registered;
    public static void register() {
        if(registered)return;registered=true;
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR,Identifier.parse("simulated:linked_typewriter_binding"),OVERLAY);
    }

    private static BlockPos clickedPos;
    private static final List<AABB> outlines = new ArrayList<>();
    private static boolean firstTick = false;

    public static void setClickedPos(final BlockPos pos) {
        clickedPos = pos;

        if (pos != null) {
            firstTick = true;
        } else {
            reset();
        }
    }

    public static void tick() {
        final ClientLevel level = Minecraft.getInstance().level;

        final LocalPlayer player = Minecraft.getInstance().player;
        final ItemStack mainHandItem = player.getMainHandItem();
        final ItemStack offhandItem = player.getOffhandItem();
        if (Minecraft.getInstance().gui.screen() != null || (!(mainHandItem.getItem() instanceof LinkedTypewriterItem) && !(offhandItem.getItem() instanceof LinkedTypewriterItem))) {
            reset();
            return;
        }

        if (firstTick || level.getGameTime() % 5 == 0) {
            firstTick = false;
            outlines.clear();

            final Couple<RedstoneLinkNetworkHandler.Frequency> frequencies = isPosValid(level);
            if (frequencies == null) {
                reset();
            } else {
                final BlockState state = level.getBlockState(clickedPos);
                final VoxelShape collisionShape = state.getShape(level, clickedPos);
                if (!collisionShape.isEmpty()) {
                    outlines.addAll(collisionShape.toAabbs());
                }
            }
        }

        if (clickedPos != null) {
            for (final AABB outline : outlines) {
                Outliner.getInstance().showAABB("linked_typewriter_outliner" + clickedPos + outline, outline.move(clickedPos))
                        .colored(SimColors.GROSS_BINDING_BROWN)
                        .lineWidth(1 / 16f);
            }
        }
    }

    public static void keyPress(final int key, final int scanCode, final int action, final int modifiers) {
        final ClientLevel level = Minecraft.getInstance().level;
        final Couple<RedstoneLinkNetworkHandler.Frequency> frequency = isPosValid(level);
        if (frequency == null) {
            reset();
            return;
        }

        if (key != InputConstants.KEY_ESCAPE) {
            final InteractionHand hand = getHand();
            if (hand != null) {
                VeilPacketManager.server().sendPacket(new TypewriterSaveKeyToItemPacket(hand, new LinkedTypewriterEntries.KeyboardEntry(frequency.getFirst(), frequency.getSecond(), key, BlockPos.ZERO)));
                LinkedTypewriterInteractionHandler.preventPress(key, scanCode);

                SimLang.builder()
                        .translate("linked_typewriter.bind_success", InputConstants.getKey(new KeyEvent(key,scanCode,modifiers)).getDisplayName().getString())
                        .sendStatus(Minecraft.getInstance().player);
            }
        }

        reset();
    }

    public static void renderOverlay(final GuiGraphicsExtractor guiGraphics, final DeltaTracker deltaTracker) {
        if (LinkedTypewriterInteractionHandler.getMode() != LinkedTypewriterInteractionHandler.Mode.BINDING_FROM_ITEM) {
            return;
        }

        final Minecraft mc = Minecraft.getInstance();
        if (mc.gui.hud.isHidden()) {
            return;
        }

        guiGraphics.pose().pushMatrix();
        final List<Component> list = new ArrayList<>();
        list.add(CreateLang.translateDirect("linked_controller.bind_mode")
                .withStyle(ChatFormatting.GOLD));

        final MutableComponent component = SimLang.translate("linked_typewriter.bind_item").component();
        list.addAll(TooltipHelper.cutTextComponent(component, FontHelper.Palette.ALL_GRAY));

        int width = 0;
        final int height = list.size() * mc.font.lineHeight;
        for (final Component iTextComponent : list) {
            width = Math.max(width, mc.font.width(iTextComponent));
        }

        final int x = (guiGraphics.guiWidth() / 3) - width / 2;
        final int y = guiGraphics.guiHeight() - height - 24;
        guiGraphics.setComponentTooltipForNextFrame(Minecraft.getInstance().font, list, x, y);

        guiGraphics.pose().popMatrix();
    }

    private static Couple<RedstoneLinkNetworkHandler.Frequency> isPosValid(final Level level) {
        Couple<RedstoneLinkNetworkHandler.Frequency> frequency = null;

        if (clickedPos != null) {
            final BlockEntity be = level.getBlockEntity(clickedPos);
            if (be instanceof final AbstractLinkedReceiverBlockEntity abe) {
                frequency = abe.getFrequency();
            }

            if (be instanceof final RedstoneLinkBlockEntity lbe) {
                frequency = lbe.getLink().getNetworkKey();
            }

        }

        return frequency;
    }

    private static InteractionHand getHand() {
        final LocalPlayer player = Minecraft.getInstance().player;
        final Item item = SimBlocks.LINKED_TYPEWRITER.asItem();
        if (player.getMainHandItem().is(item)) {
            return InteractionHand.MAIN_HAND;
        } else if (player.getOffhandItem().is(item)) {
            return InteractionHand.OFF_HAND;
        }

        return null;
    }

    public static void reset() {
        outlines.clear();
        LinkedTypewriterInteractionHandler.setMode(LinkedTypewriterInteractionHandler.Mode.IDLE);
        clickedPos = null;
    }
}
