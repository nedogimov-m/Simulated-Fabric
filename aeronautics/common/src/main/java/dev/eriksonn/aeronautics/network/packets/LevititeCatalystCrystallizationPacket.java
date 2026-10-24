package dev.eriksonn.aeronautics.network.packets;

import dev.eriksonn.aeronautics.api.levitite_blend_crystallization.LevititeBlendDummyInterface;
import dev.eriksonn.aeronautics.api.levitite_blend_crystallization.LevititeBlendHelper;
import dev.eriksonn.aeronautics.api.levitite_blend_crystallization.LevititeCrystallizerManager;
import dev.eriksonn.aeronautics.index.AeroRegistries;
import dev.eriksonn.aeronautics.index.AeroTags;
import dev.eriksonn.aeronautics.util.CatalyzerHelper;
import foundry.veil.api.network.handler.ServerPacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;

public record LevititeCatalystCrystallizationPacket(BlockPos pos, InteractionHand hand) implements CustomPacketPayload {
    public static final Type<LevititeCatalystCrystallizationPacket> TYPE = new Type<>(Identifier.parse("aeronautics:levitite_blend_crystallize"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LevititeCatalystCrystallizationPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, LevititeCatalystCrystallizationPacket::pos,
            InteractionHand.STREAM_CODEC, LevititeCatalystCrystallizationPacket::hand,
            LevititeCatalystCrystallizationPacket::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(final ServerPacketContext context) {
        final var player = context.player();
        final var level = player.level();
        final var item = player.getItemInHand(this.hand);
        if (!CatalyzerHelper.isCatalyzer(item) || player.isSpectator() || !player.mayBuild()
                || !level.isLoaded(this.pos) || !player.isWithinBlockInteractionRange(this.pos, 1)
                || !level.mayInteract(player, this.pos)) return;
        final var fluid = level.getFluidState(this.pos);
        if (!(fluid.getType() instanceof LevititeBlendDummyInterface) || !fluid.isSource()
                || LevititeCrystallizerManager.hasTicker(level, this.pos)) return;
        // Select before consuming: a final catalyst item may become an empty stack.
        final var id = Identifier.parse(item.is(AeroTags.ItemTags.LEVITITE_SOUL_CATALYZER) ? "aeronautics:soul" : "aeronautics:standard");
        final var itemContext = AeroRegistries.LEVITITE_CRYSTAL_PROPAGATION_CONTEXT.getValue(id);
        if (itemContext == null) return;
        final var spread = itemContext.getContextForSpread(level, this.pos);
        if (spread == null) return;
        LevititeBlendHelper.addLevititeBlendTicker(level, this.pos, false, false, spread);
        final var animation = item.getInteractAnimation();
        if (!item.is(AeroTags.ItemTags.LEVITITE_CATALYZER_NO_CONSUME)) {
            if (item.isDamageableItem()) item.hurtAndBreak(1, player,
                    this.hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            else if (item.isStackable() && !player.hasInfiniteMaterials()) item.shrink(1);
        }
        player.swing(this.hand, animation, true);
    }
}
