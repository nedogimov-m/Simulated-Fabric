package dev.simulated_team.simulated.index;

import dev.simulated_team.simulated.content.entities.diagram.DiagramEntity;
import dev.simulated_team.simulated.content.entities.honey_glue.HoneyGlueEntity;
import dev.simulated_team.simulated.content.entities.launched_plunger.LaunchedPlungerEntity;
import dev.simulated_team.simulated.fabric.registry.NativeRegistrations;
import dev.simulated_team.simulated.fabric.registry.NativeRegistrations.EntitySettings;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** Common registration deliberately contains no client renderer references. */
public final class SimEntityTypes {
    public static final EntityType<HoneyGlueEntity> HONEY_GLUE = NativeRegistrations.entity(
            id("honey_glue"), HoneyGlueEntity::create, MobCategory.MISC,
            new EntitySettings(10, Integer.MAX_VALUE, 0.1f, 0.1f, false, true));
    public static final EntityType<LaunchedPlungerEntity> PLUNGER = NativeRegistrations.entity(
            id("launched_plunger"), LaunchedPlungerEntity::create, MobCategory.MISC,
            new EntitySettings(10, 5, 0.5f, 0.5f, true, true));
    public static final EntityType<DiagramEntity> CONTRAPTION_DIAGRAM = NativeRegistrations.entity(
            id("contraption_diagram"), DiagramEntity::create, MobCategory.MISC,
            new EntitySettings(10, Integer.MAX_VALUE, 0.1f, 0.1f, false, true));

    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("simulated", path); }
    private SimEntityTypes() {}
    public static void register() {}
}
