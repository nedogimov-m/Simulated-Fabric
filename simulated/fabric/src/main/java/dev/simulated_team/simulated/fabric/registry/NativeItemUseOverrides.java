package dev.simulated_team.simulated.fabric.registry;

import com.simibubi.create.AllItems;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Invoke registered controls before held-item use, preserving the upstream wrench exception. */
public final class NativeItemUseOverrides {
    private static final Set<Block> BLOCKS = Collections.newSetFromMap(new IdentityHashMap<>());
    private static boolean registered;

    public static void addBlock(Block block) { BLOCKS.add(block); }
    public static void register() {
        if (registered) return;
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (player.isSpectator() || player.getItemInHand(hand).is(AllItems.WRENCH)) return InteractionResult.PASS;
            final var state = level.getBlockState(hit.getBlockPos());
            if (!BLOCKS.contains(state.getBlock())) return InteractionResult.PASS;
            final var trace = new BlockHitResult(Vec3.atCenterOf(hit.getBlockPos()), hit.getDirection(), hit.getBlockPos(), true);
            var result = state.useItemOn(player.getItemInHand(hand), level, player, hand, trace);
            if (result == InteractionResult.TRY_WITH_EMPTY_HAND) result = state.useWithoutItem(level, player, trace);
            return result.consumesAction() ? result : InteractionResult.PASS;
        });
        registered = true;
    }

    private NativeItemUseOverrides() {}
}
