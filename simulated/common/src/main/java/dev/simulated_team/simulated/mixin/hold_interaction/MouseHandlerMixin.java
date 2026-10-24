package dev.simulated_team.simulated.mixin.hold_interaction;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.simulated_team.simulated.events.SimulatedCommonClientEvents;
import dev.simulated_team.simulated.util.click_interactions.InteractCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.ScrollWheelHandler;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;
import org.joml.Vector2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @WrapMethod(method="onButton")
    private void simulated$nativeButton(long window,MouseButtonInfo event,int action,Operation<Void> original) {
        final var client=Minecraft.getInstance();
        final boolean valid=window!=0&&window==client.getWindow().handle()&&client.player!=null;
        final boolean eligible=valid&&!client.player.isSpectator();
        final boolean consumed=eligible&&SimulatedCommonClientEvents.onBeforeMouseInput(InteractCallback.Input.mouse(event.button()),event.modifiers(),action).cancelled();
        if(!consumed||action==0)original.call(window,event,action);
        if(valid)SimulatedCommonClientEvents.onAfterMouseInput(event.button(),event.modifiers(),action);
    }
    @WrapOperation(method="turnPlayer",at=@At(value="INVOKE",target="Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void simulated$nativeTurn(LocalPlayer player,double yaw,double pitch,Operation<Void> original) {
        if(player.isSpectator()||!SimulatedCommonClientEvents.onMouseMove(yaw,pitch).cancelled())original.call(player,yaw,pitch);
    }
    @WrapOperation(method="onScroll",at=@At(value="INVOKE",target="Lnet/minecraft/client/ScrollWheelHandler;onMouseScroll(DD)Lorg/joml/Vector2i;"))
    private Vector2i simulated$nativeScroll(ScrollWheelHandler wheel,double horizontal,double vertical,Operation<Vector2i> original) {
        final var player=Minecraft.getInstance().player;
        if(player!=null&&!player.isSpectator()&&SimulatedCommonClientEvents.onMouseScroll(horizontal,vertical).cancelled())return new Vector2i();
        return original.call(wheel,horizontal,vertical);
    }
}
