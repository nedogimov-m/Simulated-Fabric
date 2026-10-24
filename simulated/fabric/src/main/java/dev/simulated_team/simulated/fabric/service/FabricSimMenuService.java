package dev.simulated_team.simulated.fabric.service;

import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterBlockEntity;
import dev.simulated_team.simulated.fabric.menu.TypewriterOpeningData;
import dev.simulated_team.simulated.service.SimMenuService;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class FabricSimMenuService implements SimMenuService {
    @Override public void openScreen(ServerPlayer player, LinkedTypewriterBlockEntity typewriter) {
        if (typewriter.isRemoved() || typewriter.getLevel() != player.level()
                || player.level().getBlockEntity(typewriter.getBlockPos()) != typewriter
                || !LinkedTypewriterBlockEntity.playerInRange(player, player.level(), typewriter.getBlockPos())) return;
        player.openMenu(new ExtendedMenuProvider<TypewriterOpeningData>() {
            @Override public TypewriterOpeningData getScreenOpeningData(ServerPlayer player) {
                return new TypewriterOpeningData(typewriter.getBlockPos(), typewriter.getUpdateTag(player.registryAccess()));
            }
            @Override public Component getDisplayName() { return typewriter.getDisplayName(); }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player user) {
                return typewriter.createMenu(id, inventory, user);
            }
        });
    }
}
