package dev.ryanhcode.offroad.index;

import dev.ryanhcode.offroad.Offroad;
import dev.ryanhcode.offroad.content.ponder.scenes.BoreheadBearingScenes;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

public class OffroadPonderScenes {

    public static void register(final PonderSceneRegistrationHelper<Identifier> registry) {
        final PonderSceneRegistrationHelper<net.minecraft.world.level.ItemLike> helper = registry.withKeyFunction(item -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.asItem()));

        helper.forComponents(OffroadBlocks.BOREHEAD_BEARING_BLOCK, OffroadBlocks.ROCK_CUTTER_BLOCK)
                .addStoryBoard("borehead_bearing/intro", BoreheadBearingScenes::boreheadIntro)
                .addStoryBoard("borehead_bearing/excavating", BoreheadBearingScenes::boreheadExcavating)
                .addStoryBoard("borehead_bearing/efficiency", BoreheadBearingScenes::boreheadEfficiency);
    }

}
