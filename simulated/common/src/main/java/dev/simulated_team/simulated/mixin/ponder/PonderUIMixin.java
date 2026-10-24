package dev.simulated_team.simulated.mixin.ponder;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.simulated_team.simulated.fabric.client.gui.NativePonderShadowSnapshot;
import dev.simulated_team.simulated.mixin_interface.ponder.PonderPlatformExtension;
import dev.simulated_team.simulated.mixin_interface.ponder.PonderSceneExtension;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.ui.PonderViewport;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Capture the animated platform with its viewport; deferred submission never reads a live scene. */
@Mixin(value=PonderViewport.class,remap=false)
public abstract class PonderUIMixin {
    @Inject(method="extract",at=@At("RETURN"))
    private static void simulated$captureShadow(GuiGraphicsExtractor gui,PonderScene scene,int x,int y,
            int width,int height,double slide,float partial,float flash,CallbackInfoReturnable<PonderViewport.State> result) {
        final var extension=(PonderSceneExtension)scene;
        ((PonderPlatformExtension)(Object)result.getReturnValue().platform()).simulated$setShadow(
                new NativePonderShadowSnapshot(extension.simulated$getBasePlateAnimationTimer(partial),
                        extension.simulated$getShadowOffset(partial)));
    }

    @WrapMethod(method="submitPlatform")
    private static void simulated$submitShadow(PoseStack pose,SubmitNodeCollector collector,
                                               PonderViewport.Platform platform,Operation<Void> original) {
        pose.pushPose();
        try {
            ((PonderPlatformExtension)(Object)platform).simulated$getShadow().apply(pose);
            original.call(pose,collector,platform);
        } finally {pose.popPose();}
    }

    @ModifyArg(method="submitPlatform",at=@At(value="INVOKE",target="Lnet/createmod/ponder/foundation/ui/PonderViewport;gradient(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;FFFFII)V",ordinal=1),index=6)
    private static int simulated$shadowAlpha(int color,@Local(argsOnly=true) PonderViewport.Platform platform) {
        return ((PonderPlatformExtension)(Object)platform).simulated$getShadow().fade(color);
    }
}
