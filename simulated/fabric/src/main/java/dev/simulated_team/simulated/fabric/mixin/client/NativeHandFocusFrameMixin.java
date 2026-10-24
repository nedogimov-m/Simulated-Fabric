package dev.simulated_team.simulated.fabric.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import dev.simulated_team.simulated.fabric.client.NativeItemFocus;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GameRenderer.class)
public abstract class NativeHandFocusFrameMixin {
    @Shadow @Final private Projection hudProjection;
    @WrapMethod(method="renderItemInHand")
    private void simulated$captureNativeHandProjection(CameraRenderState camera,PlayerRenderState player,GpuTextureView depth,Operation<Void> original) {
        NativeItemFocus.begin(camera,hudProjection.getMatrix(new Matrix4f()));
        try{original.call(camera,player,depth);}finally{NativeItemFocus.end();}
    }
}
