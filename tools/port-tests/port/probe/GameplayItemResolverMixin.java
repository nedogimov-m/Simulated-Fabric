package port.probe;

import net.minecraft.client.renderer.item.*;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemModelResolver.class)
abstract class GameplayItemResolverMixin {
    @Inject(method = "appendItemLayers", at = @At("RETURN"))
    private void audit$observe(ItemStackRenderState state, ItemStack stack, ItemDisplayContext display,
            Level level, ItemOwner owner, int seed, CallbackInfo ci) {
        port.observation.GameplayItems.observe(state, stack, display);
    }
}
