package dev.simulated_team.simulated.mixin.conditional_display_target;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.api.behaviour.display.DisplayTarget;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkScreen;
import dev.simulated_team.simulated.api.ConditionalDisplayTarget;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DisplayLinkScreen.class)
public class DisplayLinkScreenMixin {
    @WrapOperation(method = "*", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/api/behaviour/display/DisplayTarget;getLineOptionText(I)Lnet/minecraft/network/chat/Component;"))
    private Component simulated$displayConditionalError(final DisplayTarget instance, final int line, final Operation<Component> original) {
        final var level = Minecraft.getInstance().level;
        final var screen = (DisplayLinkScreen) (Object) this;
        if (level == null || !(level.getBlockEntity(screen.getMenu().opening().pos()) instanceof DisplayLinkBlockEntity blockEntity)) return original.call(instance, line);
        final DisplayLinkContext context = new DisplayLinkContext(level, blockEntity);
        if (instance instanceof final ConditionalDisplayTarget cdt && !cdt.allowsWriting(context)) {
            return cdt.getErrorMessage(context).copy().withStyle(ChatFormatting.GRAY);
        }
        return original.call(instance, line);
    }
}
