package dev.simulated_team.simulated.fabric.mixin.client;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.SwingAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.SwingState.class)
public interface NativeSwingStateAccessor {
    @Invoker("start") void simulated$holdHand(InteractionHand hand,SwingAnimation animation,int duration);
}
