package dev.simulated_team.simulated.mixin.hold_interaction;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.simulated_team.simulated.events.SimulatedCommonClientEvents;
import dev.simulated_team.simulated.util.click_interactions.InteractCallback;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @WrapMethod(method="keyPress")
    private void simulated$nativeKey(long window,int action,KeyEvent event,Operation<Void> original) {
        final var client=Minecraft.getInstance();
        final boolean valid=window!=0&&window==client.getWindow().handle()&&client.player!=null;
        final boolean eligible=valid&&!client.player.isSpectator();
        final boolean consumed=eligible&&SimulatedCommonClientEvents.onBeforeMouseInput(InteractCallback.Input.key(event.key(),event.keycode()),event.modifiers(),action).cancelled();
        if(!consumed||action==0)original.call(window,action,event);
        if(valid)SimulatedCommonClientEvents.onAfterKeyPress(event.key(),event.keycode(),action,event.modifiers());
    }
}
