package dev.ryanhcode.offroad.content.ponder;

import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ItemLike;

public class OffroadPonderTags {

    public static void register(final PonderTagRegistrationHelper<Identifier> helper) {
        final PonderTagRegistrationHelper<ItemLike> itemHelper = helper.withKeyFunction(
                item -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.asItem()));
    }

}
