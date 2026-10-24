package dev.simulated_team.simulated.mixin.physics_staff;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.simulated_team.simulated.index.SimItems;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsMixin {
    @Shadow public abstract void enableScissor(int x0,int y0,int x1,int y1);
    @Shadow public abstract void disableScissor();
    @WrapMethod(method="item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V")
    private void simulated$clipStaff(LivingEntity entity,Level level,ItemStack stack,int x,int y,int seed,Operation<Void> original) {
        boolean clip=stack.is(SimItems.PHYSICS_STAFF);
        if(clip)enableScissor(x,y,x+16,y+16);
        try{original.call(entity,level,stack,x,y,seed);}finally{if(clip)disableScissor();}
    }
}
