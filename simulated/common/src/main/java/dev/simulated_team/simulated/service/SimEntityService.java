package dev.simulated_team.simulated.service;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public interface SimEntityService {

    SimEntityService INSTANCE = ServiceUtil.load(SimEntityService.class);

    CompoundTag getCustomData(Entity player);

    double getPlayerReach(Player player);

    boolean isFake(Player player);

}
