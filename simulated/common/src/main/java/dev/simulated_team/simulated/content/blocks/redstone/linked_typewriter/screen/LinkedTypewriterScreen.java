package dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.simulated_team.simulated.fabric.client.gui.NativePartsPreview;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterRenderer;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import com.simibubi.create.content.trains.schedule.ScheduleTextures;
import net.minecraft.util.LightCoordsUtil;
import com.simibubi.create.foundation.gui.AllIcons;
import dev.simulated_team.simulated.fabric.client.gui.NativeIconButton;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterBlock;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterBlockEntity;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterEntries;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterInteractionHandler;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen.widgets.ConfirmationWidgetBase;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen.widgets.KeyWidget;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.index.SimGUITextures;
import dev.simulated_team.simulated.index.SimIcons;
import dev.simulated_team.simulated.network.packets.linked_typewriter.TypewriterDisconnectUser;
import dev.simulated_team.simulated.network.packets.linked_typewriter.TypewriterKeySavePacket;
import dev.simulated_team.simulated.util.SimColors;
import foundry.veil.api.network.VeilPacketManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.joml.Vector2i;


import java.util.ArrayList;
import java.util.List;

public class LinkedTypewriterScreen extends AbstractContainerScreen<LinkedTypewriterMenuCommon> {
    public final LinkedTypewriterBlockEntity clientBe;
    private final LinkedTypewriterEntries newEntries;

    private final KeyEditorScreen keyEditorScreen;
    public final EntryModifierScreen modifier;

    private List<Rect2i> extraAreasMain;
    private final List<Rect2i> emptyExtraAreas = List.of();

    protected SimGUITextures backgroundMain;
    protected SimGUITextures backgroundBind;
    private NativeIconButton mainScreenConfirm;
    public ConfirmationWidgetBase mainScreenResetAll;
    private NativeIconButton mainScreenEditBinding;

    private boolean confirmingReset;

    private KeyRow firstKeyRow;
    private KeyRow secondKeyRow;
    private KeyRow thirdKeyRow;
    private KeyRow fourthKeyRow;
    private KeyRow fifthKeyRow;
    private final List<KeyRow> allKeys = new ArrayList<>();

    public LinkedTypewriterScreen(final LinkedTypewriterMenuCommon container, final Inventory inv, final Component title) {
        super(container, inv, title, SimGUITextures.LINKED_TYPEWRITER_MAIN.width, SimGUITextures.LINKED_TYPEWRITER_MAIN.height);

        this.clientBe = container.contentHolder;
        this.newEntries = new LinkedTypewriterEntries();
        this.newEntries.addAll(this.clientBe.getTypewriterEntries().getKeyMap());

//        this.editor2 = new KeyEditorScreen(this);

        this.keyEditorScreen = new KeyEditorScreen(this);
        this.modifier = new EntryModifierScreen(this);
    }

    @Override
    protected void init() {
        this.backgroundMain = SimGUITextures.LINKED_TYPEWRITER_MAIN;
        this.backgroundBind = SimGUITextures.LINKED_TYPEWRITER_BIND;

        super.init();

        this.rebuildExtraAreas();

        final Vector2i pos = new Vector2i(145, 102);
        final int spacing = 8;

        this.firstKeyRow = new KeyRow(pos.x(), pos.y(), this.clientBe);
        this.secondKeyRow = new KeyRow(pos.x(), pos.y() + spacing, this.clientBe);
        this.thirdKeyRow = new KeyRow(pos.x(), pos.y() + spacing * 2, this.clientBe);
        this.fourthKeyRow = new KeyRow(pos.x(), pos.y() + spacing * 3, this.clientBe);
        this.fifthKeyRow = new KeyRow(pos.x(), pos.y() + spacing * 4, this.clientBe);
        this.setRows();

        this.modifier.init();
//        this.editor2.init(); //we no longer init like this...

        //Resets all keys and resets serverside TypeWriter
        this.mainScreenResetAll = new ConfirmationWidgetBase(this.getLeftPos() + 8, this.getTopPos() + this.backgroundMain.height - 24, AllIcons.I_TRASH)
                .withMessage(SimLang.translate("linked_typewriter.confirm_delete_all").component())
                .withCallback(() -> this.sendNewKeys(true));

        //Confirms the current modifications and sends the keys to the server
        this.mainScreenConfirm = new NativeIconButton(this.getLeftPos() + this.backgroundMain.width - 33, this.getTopPos() + this.backgroundMain.height - 24, AllIcons.I_CONFIRM).withCallback(this::onClose);

        //Opens up the binding screen
        this.mainScreenEditBinding = new NativeIconButton(this.getLeftPos() + this.backgroundMain.width - 62, this.getTopPos() + this.backgroundMain.height - 24, SimIcons.HAMBURGER).withCallback(() -> {
            this.switchScreen(true);
        });

        this.addWidget(this.mainScreenResetAll);
        this.addWidget(this.mainScreenConfirm);
        this.addWidget(this.mainScreenEditBinding);

        for (final KeyRow keyRow : this.allKeys) {
            for (final KeyWidget kwid : keyRow) {
                this.addWidget(kwid);
            }
        }
    }

    @Override
    protected void rebuildWidgets() {
        this.clearFocus();

        this.rescaleWindow();
        this.keyEditorScreen.resetPositions();
        this.modifier.resetXYPositions();

        this.setInitialFocus();
    }

    private void rebuildExtraAreas() {
        this.extraAreasMain = ImmutableList.of(this.getTypewriterBlockRect());
    }

    public Rect2i getTypewriterBlockRect() {
        return new Rect2i(this.leftPos + this.backgroundMain.width - 30, this.topPos + this.backgroundMain.height - 30, 94, 94);
    }

    public void rescaleWindow() {
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        this.backgroundMain = SimGUITextures.LINKED_TYPEWRITER_MAIN;
        this.backgroundBind = SimGUITextures.LINKED_TYPEWRITER_BIND;

        this.rebuildExtraAreas();

        final int widgetHeight = this.getTopPos() + this.backgroundMain.height - 24;
        this.mainScreenResetAll.setX(this.getLeftPos() + 8);
        this.mainScreenResetAll.setY(widgetHeight);

        this.mainScreenConfirm.setX(this.getLeftPos() + this.backgroundMain.width - 33);
        this.mainScreenConfirm.setY(widgetHeight);


        this.mainScreenEditBinding.setX(this.getLeftPos() + this.backgroundMain.width - 62);
        this.mainScreenEditBinding.setY(widgetHeight);
    }

    /**
     * Switches between the binding screen and the main screen of this typewriter
     */
    public void switchScreen(final boolean subScreen) {
        this.clearWidgets();

        if (subScreen) {
            this.keyEditorScreen.startEditing();
        } else {
            this.keyEditorScreen.endEditing();

            for (final KeyRow keyRow : this.allKeys) {
                for (final KeyWidget kwid : keyRow) {
                    this.addWidget(kwid);
                }
            }

            this.addWidget(this.mainScreenResetAll);
            this.mainScreenResetAll.confirmation = false;
            this.mainScreenResetAll.setX(this.getLeftPos() + 8);
            this.mainScreenResetAll.setY(this.getTopPos() + this.backgroundMain.height - 24);

            this.addWidget(this.mainScreenConfirm);
            this.addWidget(this.mainScreenEditBinding);
        }
    }

    @Override
    public <T extends GuiEventListener & NarratableEntry> T addWidget(final T listener) {
        return super.addWidget(listener);
    }

    @Override
    public void removeWidget(final GuiEventListener listener) {
        super.removeWidget(listener);
    }

    @Override
    public void onClose() {
        if (this.modifier.psuedoEntry != null) {
            this.modifier.psuedoEntry.finishModifications();
        }

        this.sendNewKeys(false);

        LinkedTypewriterInteractionHandler.setMode(LinkedTypewriterInteractionHandler.Mode.IDLE);
        LinkedTypewriterInteractionHandler.associateTypewriter(null);

        VeilPacketManager.server().sendPacket(new TypewriterDisconnectUser(this.clientBe.getBlockPos()));
        super.onClose();
    }

    public void sendNewKeys(final boolean clearServer) {
        if (clearServer) {
            this.newEntries.clearAll();
        }

        // save keys!
        VeilPacketManager.server().sendPacket(new TypewriterKeySavePacket(this.newEntries, this.clientBe.getBlockPos(), clearServer));
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY) {
        if (this.keyEditorScreen.active && !this.modifier.modifying) {
            this.keyEditorScreen.shiftEntries(scrollY > 0);
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void containerTick() {
        super.containerTick();

        if (this.keyEditorScreen.active) {
            this.keyEditorScreen.tick();
        }
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor gui, final int mouseX, final int mouseY, final float partial) {
        super.extractRenderState(gui, mouseX, mouseY, partial);
        if (this.keyEditorScreen.active) this.keyEditorScreen.render(gui, mouseX, mouseY, partial, gui.pose());
        if (this.modifier.modifying) {
            gui.nextStratum();
            this.modifier.render(gui, mouseX, mouseY, partial, gui.pose());
        }
        extractTooltip(gui, mouseX, mouseY);
    }

    @Override protected void extractLabels(GuiGraphicsExtractor gui, int mouseX, int mouseY) {}

    public void renderPlayerInventory(GuiGraphicsExtractor gui, int x, int y) {
        ScheduleTextures.PLAYER_INVENTORY.render(gui, x, y);
        gui.text(font, playerInventoryTitle, x + 8, y + 6, 0xff404040, false);
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor guiGraphics, final int mx, final int my, final float pt) {
        super.extractBackground(guiGraphics, mx, my, pt);
        final int titleX = (this.backgroundMain.width / 2) - (Minecraft.getInstance().font.width(this.getTitle()) / 2);

        if (!this.keyEditorScreen.active) {
            this.backgroundMain.render(guiGraphics, this.getLeftPos(), this.getTopPos());
            guiGraphics.text(Minecraft.getInstance().font, this.getTitle(), this.getLeftPos() + titleX, this.getTopPos() + 4, 0xff000000 | SimColors.TITLE_DARK_RED, false);

            final int x = this.leftPos;
            final int y = this.topPos;

            final int rx = x + 8;
            final int ry = y + 21;


            if (!this.modifier.modifying) {
                this.renderTypeWriter(guiGraphics, x, y, pt);
            }

            int i = 0;
            for (final KeyRow keyRow : this.allKeys) {
                keyRow.render(guiGraphics, rx, ry + i * 14, mx, my, pt, true);
                i++;
            }

            this.mainScreenResetAll.extractRenderState(guiGraphics, mx, my, pt);
            this.mainScreenConfirm.extractRenderState(guiGraphics, mx, my, pt);
            this.mainScreenEditBinding.extractRenderState(guiGraphics, mx, my, pt);
        } else {
            this.keyEditorScreen.renderBG(guiGraphics, pt, mx, my);
        }

        if (this.modifier.modifying) {
            this.modifier.renderBG(guiGraphics);
        }
    }

    private void renderTypeWriter(final GuiGraphicsExtractor gui, final int x, final int y, final float partial) {
        final var area = getTypewriterBlockRect();
        final var parts = new NativePartsRenderer.Parts(LightCoordsUtil.FULL_BRIGHT);
        final var pose = new PoseStack();
        pose.translate(x + backgroundMain.width + 4 - area.getX(), y + backgroundMain.height + 4 - area.getY(), 100);
        pose.scale(40, 40, 40);
        pose.rotate(Axis.XP.rotationDegrees(-22));
        pose.rotate(Axis.YP.rotationDegrees(63));
        pose.pushPose();
        pose.scale(1, -1, 1); // Original GuiGameElement block-space reflection.
        final net.minecraft.world.level.block.state.BlockState previewState = clientBe.getBlockState()
                .setValue(LinkedTypewriterBlock.HORIZONTAL_FACING, Direction.WEST);
        parts.add(previewState, pose);
        pose.popPose();
        pose.scale(-1, -1, -1);
        pose.translate(-1, 0, -1);
        NativePartsRenderer.rotateCentered(pose, Direction.Axis.Y, -90);
        NativePartsRenderer.rotateCentered(pose, Direction.Axis.Y,
                clientBe.getBlockState().getValue(LinkedTypewriterBlock.FACING).getOpposite().toYRot());
        LinkedTypewriterRenderer.extractKeys(clientBe, partial, parts, pose, 255);
        NativePartsPreview.extract(gui, parts, area.getX(), area.getY(), area.getWidth(), area.getHeight());
    }

    private void switchStates(final boolean newState) {
        for (final KeyRow allKey : LinkedTypewriterScreen.this.allKeys) {
            for (final KeyWidget key : allKey) {
                key.setActive(newState);
            }
        }

        LinkedTypewriterScreen.this.mainScreenEditBinding.setActive(newState);
        LinkedTypewriterScreen.this.mainScreenResetAll.setActive(newState);
        LinkedTypewriterScreen.this.mainScreenConfirm.setActive(newState);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        final double pMouseX = event.x(), pMouseY = event.y(); final int pButton = event.button();
        final int x = this.leftPos;
        final int y = this.topPos;
        if (this.confirmingReset && !(pMouseX > x + 8 && pMouseX < x + 26 && pMouseY > y + this.backgroundMain.height - 24 && pMouseY < y + this.backgroundMain.height - 6)) {
            this.confirmingReset = false;
        }

        return super.mouseClicked(event, doubleClick);
    }

    public LinkedTypewriterEntries getNewEntries() {
        return this.newEntries;
    }

    public List<Rect2i> getExtraAreas() {
        if (this.keyEditorScreen.active || this.modifier.modifying) {
            return this.emptyExtraAreas;
        } else {
            return this.extraAreasMain;
        }
    }

    public int getTopPos() {
        return this.topPos;
    }

    public int getLeftPos() {
        return this.leftPos;
    }

    public void setRows() {
        final int standardLength = 14;

        this.firstKeyRow.add(standardLength, InputConstants.KEY_GRAVE, null);
        this.firstKeyRow.add(standardLength, InputConstants.KEY_1, null);
        this.firstKeyRow.add(standardLength, InputConstants.KEY_2, null);
        this.firstKeyRow.add(standardLength, InputConstants.KEY_3, null);
        this.firstKeyRow.add(standardLength, InputConstants.KEY_4, null);
        this.firstKeyRow.add(standardLength, InputConstants.KEY_5, null);
        this.firstKeyRow.add(standardLength, InputConstants.KEY_6, null);
        this.firstKeyRow.add(standardLength, InputConstants.KEY_7, null);
        this.firstKeyRow.add(standardLength, InputConstants.KEY_8, null);
        this.firstKeyRow.add(standardLength, InputConstants.KEY_9, null);
        this.firstKeyRow.add(standardLength, InputConstants.KEY_0, null);
        this.firstKeyRow.add(standardLength, InputConstants.KEY_MINUS, null);
        this.firstKeyRow.add(standardLength, InputConstants.KEY_EQUALS, null);
        this.firstKeyRow.add(standardLength + 12, InputConstants.KEY_BACKSPACE, null);
        this.firstKeyRow.add(standardLength, InputConstants.KEY_DELETE, null);

        this.secondKeyRow.add(standardLength + 6, InputConstants.KEY_TAB, null);
        this.secondKeyRow.add(standardLength, InputConstants.KEY_Q, null);
        this.secondKeyRow.add(standardLength, InputConstants.KEY_W, null);
        this.secondKeyRow.add(standardLength, InputConstants.KEY_E, null);
        this.secondKeyRow.add(standardLength, InputConstants.KEY_R, null);
        this.secondKeyRow.add(standardLength, InputConstants.KEY_T, null);
        this.secondKeyRow.add(standardLength, InputConstants.KEY_Y, null);
        this.secondKeyRow.add(standardLength, InputConstants.KEY_U, null);
        this.secondKeyRow.add(standardLength, InputConstants.KEY_I, null);
        this.secondKeyRow.add(standardLength, InputConstants.KEY_O, null);
        this.secondKeyRow.add(standardLength, InputConstants.KEY_P, null);
        this.secondKeyRow.add(standardLength, InputConstants.KEY_LBRACKET, null);
        this.secondKeyRow.add(standardLength, InputConstants.KEY_RBRACKET, null);
        this.secondKeyRow.add(standardLength + 6, InputConstants.KEY_BACKSLASH, null);
        this.secondKeyRow.add(standardLength, InputConstants.KEY_PAGEUP, null);

        this.thirdKeyRow.add(standardLength + 12, InputConstants.KEY_CAPSLOCK, null);
        this.thirdKeyRow.add(standardLength, InputConstants.KEY_A, null);
        this.thirdKeyRow.add(standardLength, InputConstants.KEY_S, null);
        this.thirdKeyRow.add(standardLength, InputConstants.KEY_D, null);
        this.thirdKeyRow.add(standardLength, InputConstants.KEY_F, null);
        this.thirdKeyRow.add(standardLength, InputConstants.KEY_G, null);
        this.thirdKeyRow.add(standardLength, InputConstants.KEY_H, null);
        this.thirdKeyRow.add(standardLength, InputConstants.KEY_J, null);
        this.thirdKeyRow.add(standardLength, InputConstants.KEY_K, null);
        this.thirdKeyRow.add(standardLength, InputConstants.KEY_L, null);
        this.thirdKeyRow.add(standardLength, InputConstants.KEY_SEMICOLON, null);
        this.thirdKeyRow.add(standardLength, InputConstants.KEY_APOSTROPHE, null);
        this.thirdKeyRow.add(standardLength + 14, InputConstants.KEY_RETURN, null);
        this.thirdKeyRow.add(standardLength, InputConstants.KEY_PAGEDOWN, null);

        this.fourthKeyRow.add(standardLength + 18, InputConstants.KEY_LSHIFT, null);
        this.fourthKeyRow.add(standardLength, InputConstants.KEY_Z, null);
        this.fourthKeyRow.add(standardLength, InputConstants.KEY_X, null);
        this.fourthKeyRow.add(standardLength, InputConstants.KEY_C, null);
        this.fourthKeyRow.add(standardLength, InputConstants.KEY_V, null);
        this.fourthKeyRow.add(standardLength, InputConstants.KEY_B, null);
        this.fourthKeyRow.add(standardLength, InputConstants.KEY_N, null);
        this.fourthKeyRow.add(standardLength, InputConstants.KEY_M, null);
        this.fourthKeyRow.add(standardLength, InputConstants.KEY_COMMA, null);
        this.fourthKeyRow.add(standardLength, InputConstants.KEY_PERIOD, null);
        this.fourthKeyRow.add(standardLength, InputConstants.KEY_SLASH, null);
        this.fourthKeyRow.add(standardLength + 8, InputConstants.KEY_RSHIFT, null);
        this.fourthKeyRow.add(standardLength, InputConstants.KEY_UP, SimIcons.KEY_ARROW_UP);
        this.fourthKeyRow.add(standardLength, InputConstants.KEY_END, null);

        this.fifthKeyRow.add(standardLength + 4, InputConstants.KEY_LCONTROL, null);
        this.fifthKeyRow.add(standardLength, InputConstants.KEY_LGUI, null);
        this.fifthKeyRow.add(standardLength, InputConstants.KEY_LALT, null);
        this.fifthKeyRow.add(standardLength + 74, InputConstants.KEY_SPACE, null);
        this.fifthKeyRow.add(standardLength, InputConstants.KEY_RALT, null);
        this.fifthKeyRow.add(standardLength, org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_APPLICATION, null);
        this.fifthKeyRow.add(standardLength + 4, InputConstants.KEY_RCONTROL, null);
        this.fifthKeyRow.add(standardLength, InputConstants.KEY_LEFT, SimIcons.KEY_ARROW_LEFT);
        this.fifthKeyRow.add(standardLength, InputConstants.KEY_DOWN, SimIcons.KEY_ARROW_DOWN);
        this.fifthKeyRow.add(standardLength, InputConstants.KEY_RIGHT, SimIcons.KEY_ARROW_RIGHT);

        this.allKeys.clear();
        this.allKeys.add(this.firstKeyRow);
        this.allKeys.add(this.secondKeyRow);
        this.allKeys.add(this.thirdKeyRow);
        this.allKeys.add(this.fourthKeyRow);
        this.allKeys.add(this.fifthKeyRow);
    }

    private class KeyRow extends ArrayList<KeyWidget> {
        Vector2i pos;
        LinkedTypewriterBlockEntity be;

        public KeyRow(final int x, final int y, final LinkedTypewriterBlockEntity be) {
            super();
            this.pos = new Vector2i(x, y);
            this.be = be;
        }

        public void add(final int length, final int glfwKey, final SimIcons icon) {
            final KeyWidget kWid = new KeyWidget(2, 2, length, glfwKey, icon, LinkedTypewriterScreen.this);

            //whenever the key is pressed, we want to open up the subscreen with the correct index
            kWid.withCallback(() -> {
                LinkedTypewriterScreen.this.switchStates(false);

                LinkedTypewriterScreen.this.modifier.startModifying(LinkedTypewriterScreen.this.newEntries.getEntry(glfwKey), (newEntry) -> {
                    LinkedTypewriterScreen.this.switchStates(true);

                    if (newEntry != null) {
                        LinkedTypewriterScreen.this.getNewEntries().setKey(newEntry.scanCode, newEntry);
                    }
                }).keyCode(glfwKey);
            });

            this.add(kWid);
        }

        public void render(final GuiGraphicsExtractor guiGraphics, final int x, final int y, final int mouseX, final int mouseY, final float pt, final boolean keyboardActive) {
            int length = 0;
            for (final KeyWidget key : this) {
                key.render(guiGraphics, x + length, y, mouseX, mouseY, pt, keyboardActive);

                length += key.getWidth();
            }
        }
    }
}