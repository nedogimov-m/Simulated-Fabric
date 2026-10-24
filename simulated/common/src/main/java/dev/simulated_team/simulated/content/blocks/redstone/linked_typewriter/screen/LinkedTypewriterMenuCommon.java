package dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen;

import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterBlockEntity;
import dev.simulated_team.simulated.fabric.menu.NativeGhostMenu;
import dev.simulated_team.simulated.fabric.menu.TypewriterOpeningData;
import dev.simulated_team.simulated.index.SimMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

public final class LinkedTypewriterMenuCommon extends NativeGhostMenu<LinkedTypewriterBlockEntity> {
    public LinkedTypewriterMenuCommon(MenuType<?> type, int id, Inventory inventory, LinkedTypewriterBlockEntity be) {
        super(type, id, inventory, be, player -> !be.isRemoved() && be.getLevel() == player.level()
                && player.level().getBlockEntity(be.getBlockPos()) == be
                && LinkedTypewriterBlockEntity.playerInRange(player, player.level(), be.getBlockPos()),
                2, false, 38, 59, 105, 1);
    }

    public static LinkedTypewriterMenuCommon create(int id, Inventory inventory, LinkedTypewriterBlockEntity be) {
        return new LinkedTypewriterMenuCommon(SimMenuTypes.LINKED_TYPEWRITER, id, inventory, be);
    }

    public static LinkedTypewriterMenuCommon fromOpening(int id, Inventory inventory, TypewriterOpeningData data) {
        if (!(inventory.player.level().getBlockEntity(data.pos()) instanceof LinkedTypewriterBlockEntity be)) {
            throw new IllegalStateException("Missing linked typewriter at " + data.pos());
        }
        be.readClient(data.tag(), inventory.player.registryAccess());
        return create(id, inventory, be);
    }
}
