package dev.simulated_team.simulated.fabric.entity;

import dev.simulated_team.simulated.service.SimEntityService;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Persistent entity data and player access use Fabric/vanilla facilities on both physical sides. */
public final class FabricSimEntityService implements SimEntityService {
    private static final AttachmentType<CompoundTag> CUSTOM_DATA = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath("simulated", "entity_data"),
            builder -> builder.initializer(CompoundTag::new).persistent(CompoundTag.CODEC));

    @Override
    public CompoundTag getCustomData(Entity entity) {
        return ((AttachmentTarget) entity).getAttachedOrCreate(CUSTOM_DATA);
    }

    @Override
    public double getPlayerReach(Player player) {
        return player.blockInteractionRange();
    }

    @Override
    public boolean isFake(Player player) {
        return player instanceof FakePlayer;
    }
}
