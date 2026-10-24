package dev.simulated_team.simulated.fabric.client;

import dev.simulated_team.simulated.events.SimulatedCommonClientEvents;
import dev.simulated_team.simulated.index.SimClickInteractions;
import dev.simulated_team.simulated.index.SimItems;
import dev.simulated_team.simulated.index.SimKeys;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;

/** Actual Fabric owners for the source client ticks, hold HUD, key registration, tooltip and use callbacks. */
public final class NativeSimClientEvents {
    private static boolean registered;
    public static void register() {
        if(registered)return;registered=true;
        SimKeys.registerTo(KeyMappingHelper::registerKeyMapping);
        ClientTickEvents.START_CLIENT_TICK.register(SimulatedCommonClientEvents::preClientTick);
        ClientTickEvents.END_CLIENT_TICK.register(SimulatedCommonClientEvents::postClientTick);
        HudElementRegistry.attachElementAfter(VanillaHudElements.SUBTITLES,Identifier.parse("simulated:hold_overlays"),(gui,delta)->
                SimulatedCommonClientEvents.renderOverlays(gui,delta.getGameTimeDeltaPartialTick(false)));
        ItemTooltipCallback.EVENT.register((stack,context,flag,lines)->SimulatedCommonClientEvents.appendTooltip(stack,flag,Minecraft.getInstance().player,lines));
        UseBlockCallback.EVENT.register((player,level,hand,hit)->{
            if(!level.isClientSide())return InteractionResult.PASS;
            final var held=SimulatedCommonClientEvents.onRightClickBlock(player,hand,hit.getBlockPos(),hit);if(held!=null)return held;
            final var stack=player.getItemInHand(hand);
            if(stack.is(SimItems.HONEY_GLUE)){SimClickInteractions.HONEY_GLUE_MANAGER.selectPos(hit.getBlockPos(),player,stack);return InteractionResult.SUCCESS;}
            return InteractionResult.PASS;
        });
        UseItemCallback.EVENT.register((player,level,hand)->{
            if(level.isClientSide())SimulatedCommonClientEvents.useItemOnAirEvent(level,player,player.getItemInHand(hand),hand);
            return InteractionResult.PASS;
        });
    }
    private NativeSimClientEvents() { }
}
