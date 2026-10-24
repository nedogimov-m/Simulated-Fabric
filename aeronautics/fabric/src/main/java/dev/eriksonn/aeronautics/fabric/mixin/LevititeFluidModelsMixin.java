package dev.eriksonn.aeronautics.fabric.mixin;

import dev.eriksonn.aeronautics.fabric.fluid.LevititeFluidClient;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Map;

@Mixin(FluidStateModelSet.class)
abstract class LevititeFluidModelsMixin {
    @Inject(method = "bake", at = @At("RETURN"), cancellable = true)
    private static void aeronautics$models(MaterialBaker baker, CallbackInfoReturnable<Map<Fluid, FluidModel>> callback) {
        callback.setReturnValue(LevititeFluidClient.bake(baker, callback.getReturnValue()));
    }
}
