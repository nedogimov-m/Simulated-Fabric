package dev.simulated_team.simulated.fabric.registry;

import com.simibubi.create.content.equipment.extendoGrip.ExtendoGripItem;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.world.InteractionResult;

/** The original item's creative attack veto, through the actual native player interaction event. */
public final class NativeExtendoGripRules {
    private static boolean registered;
    public static void register(){
        if(registered)return;registered=true;
        AttackBlockCallback.EVENT.register((player,level,hand,pos,direction)->player.isCreative()
                &&player.getItemInHand(hand).getItem() instanceof ExtendoGripItem?InteractionResult.FAIL:InteractionResult.PASS);
    }
    private NativeExtendoGripRules(){}
}
