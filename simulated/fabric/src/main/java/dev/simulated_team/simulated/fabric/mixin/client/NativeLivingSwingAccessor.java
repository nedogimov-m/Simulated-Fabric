package dev.simulated_team.simulated.fabric.mixin.client;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.SwingAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Native swing owner access for the source handle's continuous local animation, without per-tick packets. */
@Mixin(LivingEntity.class)
public interface NativeLivingSwingAccessor {
    @Accessor("swingState") LivingEntity.SwingState simulated$swingState();
    @Invoker("getModifiedSwingDuration") int simulated$swingDuration(SwingAnimation animation);
}
