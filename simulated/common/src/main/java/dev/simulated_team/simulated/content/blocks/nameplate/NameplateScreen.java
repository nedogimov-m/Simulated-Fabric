package dev.simulated_team.simulated.content.blocks.nameplate;

import org.joml.Matrix3x2fStack;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.network.packets.name_plate.NameplateChangeNamePacket;
import foundry.veil.api.network.VeilPacketManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

public class NameplateScreen extends Screen {
   public static final int MAX_WIDTH = 8 * 16 - 6;
   private final NameplateBlockEntity be;
   private String message;
   private int frame;
   @Nullable
   private TextFieldHelper nameField;

   private Button button;

   public NameplateScreen(final NameplateBlockEntity pSign) {
      this(pSign, SimLang.translate("nameplate.edit").component());
   }

   public NameplateScreen(final NameplateBlockEntity pSign, final Component pTitle) {
      super(pTitle);
      this.be = pSign;
      this.message = pSign.getName();
   }

   @Override
   protected void init() {
       this.button = this.addWidget(Button.builder(CommonComponents.GUI_DONE,
              (button) -> this.onDone()).bounds(this.width / 2 - 100,
              this.height / 4 + 144,
              200,
              20).build());

      this.nameField = new TextFieldHelper(() -> this.message,
              this::setMessage,
              TextFieldHelper.createClipboardGetter(this.minecraft),
              TextFieldHelper.createClipboardSetter(this.minecraft),
              (string) -> this.minecraft.font.width(string) <= MAX_WIDTH);
   }

   private void setMessage(final String s) {
      this.message = s;
   }

   @Override
   public void tick() {
      ++this.frame;
      if (!this.isValid()) {
         this.onDone();
      }
   }

   private boolean isValid() {
      return this.minecraft != null &&
              this.minecraft.player != null &&
              !this.be.isRemoved() &&
              NameplateBlockEntity.canPlayerReach(this.be, this.minecraft.player);
   }

   @Override
   public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        final int pKeyCode = event.key(), pScanCode = event.keycode(), pModifiers = event.modifiers();
      if (pKeyCode != 264 && pKeyCode != 257 && pKeyCode != 335) {
         return this.nameField.keyPressed(event) || super.keyPressed(event);
      }
      return false;
   }

   @Override
   public boolean charTyped(final net.minecraft.client.input.CharacterEvent event) {
      this.nameField.charTyped(event);
      return true;
   }

   @Override
   public void extractRenderState(final GuiGraphicsExtractor gui, final int pMouseX, final int pMouseY, final float pPartialTick) {
      gui.centeredText(this.font, this.title, this.width / 2, 40, 0xffffffff);

      this.renderSign(gui);

      // The native Screen wrapper extracts the background before this content.
      this.button.extractRenderState(gui, pMouseX, pMouseY, pPartialTick);
   }

   @Override
   public void onClose() {
      this.onDone();
   }

   @Override
   public void removed() {
      VeilPacketManager.server().sendPacket(new NameplateChangeNamePacket(this.be.findController().getBlockPos(), this.message));
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   protected void renderSignBackground(final GuiGraphicsExtractor gui, final BlockState pState) {
      final String color = ((NameplateBlock) pState.getBlock()).getColor().getSerializedName();

      final Matrix3x2fStack ps = gui.pose();

      ps.pushMatrix();
      final float sy = 15.0f / 12.0f;
      ps.scale(sy, sy);
      ps.translate(8.0f - 16.0f * 4, 5.7f);

      gui.blit(RenderPipelines.GUI_TEXTURED, Simulated.path("textures/block/nameplate/" + color + "_nameplate.png"), -8, -8, 0.0F, 12, 16, 10, 32, 32);

      for (int i = 0; i < 6; i++) {
         ps.translate(16.0f, 0.0f);
         gui.blit(RenderPipelines.GUI_TEXTURED, Simulated.path("textures/block/nameplate/" + color + "_nameplate.png"), -8, -8,  8, 12, 16, 10, 32, 32);
      }
      ps.translate(16.0f, 0.0f);
      gui.blit(RenderPipelines.GUI_TEXTURED, Simulated.path("textures/block/nameplate/" + color + "_nameplate.png"), -8, -8, 16, 12, 16, 10, 32, 32);

      ps.popMatrix();
   }

   protected void offsetSign(final GuiGraphicsExtractor pGuiGraphics, final BlockState pState) {
      pGuiGraphics.pose().translate((float)this.width / 2.0F, this.height / 2f - 26);
   }

   private void renderSign(final GuiGraphicsExtractor pGuiGraphics) {
      final Matrix3x2fStack ps = pGuiGraphics.pose();

      ps.pushMatrix();

      final BlockState blockstate = this.be.getBlockState();
      pGuiGraphics.pose().pushMatrix();
      this.offsetSign(pGuiGraphics, blockstate);
      final float scale = 2.0f;
      ps.scale(scale, scale);
      pGuiGraphics.pose().pushMatrix();
      this.renderSignBackground(pGuiGraphics, blockstate);
      pGuiGraphics.pose().popMatrix();
      this.renderSignText(pGuiGraphics);
      pGuiGraphics.pose().popMatrix();

      ps.popMatrix();
   }

   private void renderSignText(final GuiGraphicsExtractor pGuiGraphics) {
      final int lineHeight = 8;
      // Extraction preserves draw order; text follows the sign background.

      final int color = 0xff000000 | this.be.getDarkColor(this.be.getTextColor());

      final boolean cursorFlash = this.frame / 6 % 2 == 0;
      final int cursorPos = this.nameField.getCursorPos();
      final int selectionPos = this.nameField.getSelectionPos();

      if (this.message != null) {
         if (this.font.isBidirectional()) {
             this.message = this.font.bidirectionalShaping(this.message);
         }

         final int w = -this.font.width(this.message) / 2;
         pGuiGraphics.text(this.font, this.message, w, 0, color, false);
         if (cursorPos >= 0 && cursorFlash) {
            final int l1 = this.font.width(this.message.substring(0, Math.max(Math.min(cursorPos, this.message.length()), 0)));
            final int i2 = l1 - this.font.width(this.message) / 2;
            if (cursorPos >= this.message.length()) {
               pGuiGraphics.text(this.font, "_", i2, 0, color, false);
            }
         }
      }

      if (this.message != null && cursorPos >= 0) {
         final int width = this.font.width(this.message.substring(0, Math.max(Math.min(cursorPos, this.message.length()), 0)));
         final int cen = width - this.font.width(this.message) / 2;
         if (cursorFlash && cursorPos < this.message.length()) {
            pGuiGraphics.fill(cen, -1, cen + 1, lineHeight, -16777216 | color);
         }

         if (selectionPos != cursorPos) {
            final int min = Math.min(cursorPos, selectionPos);
            final int max = Math.max(cursorPos, selectionPos);
            final int minWidth = this.font.width(this.message.substring(0, min)) - this.font.width(this.message) / 2;
            final int maxWith = this.font.width(this.message.substring(0, max)) - this.font.width(this.message) / 2;
            final int selMin = Math.min(minWidth, maxWith);
            final int selMax = Math.max(minWidth, maxWith);
            pGuiGraphics.textHighlight(selMin, -1, selMax, lineHeight, false);
         }
      }
   }

   private void onDone() {
      this.minecraft.setScreenAndShow(null);
   }

   public static void setScreen(final NameplateBlockEntity be) {
      if (be != null && NameplateBlockEntity.canPlayerReach(be, Minecraft.getInstance().player)) {
         final NameplateScreen screen = new NameplateScreen(be.findController());
         Minecraft.getInstance().setScreenAndShow(screen);
      }
   }
}
