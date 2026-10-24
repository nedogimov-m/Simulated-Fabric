package dev.simulated_team.simulated.service;

import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterBlockEntity;
import net.minecraft.server.level.ServerPlayer;

public interface SimMenuService {
    SimMenuService INSTANCE = ServiceUtil.load(SimMenuService.class);
    void openScreen(ServerPlayer player, LinkedTypewriterBlockEntity typewriter);
}
