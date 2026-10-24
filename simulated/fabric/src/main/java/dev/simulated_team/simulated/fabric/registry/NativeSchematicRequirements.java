package dev.simulated_team.simulated.fabric.registry;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.schematic.requirement.SchematicRequirementRegistries;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;

/** Native schematic material for the engine-owned shaft variant, which has no separate inventory item. */
public final class NativeSchematicRequirements {
    private static boolean registered;

    public static void register() {
        if (registered) return;
        SchematicRequirementRegistries.BLOCKS.register(AllBlocks.POWERED_SHAFT,
                (state, entity) -> new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, AllBlocks.SHAFT.asItem()));
        registered = true;
    }

    private NativeSchematicRequirements() {}
}
