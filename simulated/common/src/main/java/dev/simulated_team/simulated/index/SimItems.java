package dev.simulated_team.simulated.index;

import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import dev.simulated_team.simulated.content.entities.diagram.DiagramItem;
import dev.simulated_team.simulated.content.entities.honey_glue.HoneyGlueItem;
import dev.simulated_team.simulated.content.items.plunger_launcher.PlungerLauncherItem;
import dev.simulated_team.simulated.content.items.rope.RopeItem.RopeItem;
import dev.simulated_team.simulated.content.items.spring.SpringItem;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffItem;
import dev.simulated_team.simulated.fabric.registry.NativeItemCatalog;
import dev.simulated_team.simulated.fabric.registry.NativeItemCatalog.Visibility;
import dev.simulated_team.simulated.fabric.registry.NativeRegistrations;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.function.Function;

/** Actual runtime items; the original checked-in item models, tags and recipes remain authoritative. */
public final class SimItems {
    private static final Identifier SECTION = id("simulated");

    public static final DiagramItem CONTRAPTION_DIAGRAM = item("contraption_diagram", DiagramItem::new, new Item.Properties());
    public static final SpringItem SPRING = item("spring", SpringItem::new,
            new Item.Properties().component(SimDataComponents.BOUNCINESS, 1f));
    public static final RopeItem ROPE_COUPLING = item("rope_coupling", RopeItem::new, new Item.Properties());
    public static final Item GYRO_MECHANISM = item("gyroscopic_mechanism", Item::new, new Item.Properties());
    public static final SequencedAssemblyItem INCOMPLETE_GYRO_MECHANISM = item("incomplete_gyroscopic_mechanism",
            SequencedAssemblyItem::new, new Item.Properties(), Visibility.INVISIBLE);
    public static final Item ENGINE_ASSEMBLY = item("engine_assembly", Item::new, new Item.Properties());
    public static final SequencedAssemblyItem INCOMPLETE_ENGINE_ASSEMBLY = item("incomplete_engine_assembly",
            SequencedAssemblyItem::new, new Item.Properties(), Visibility.INVISIBLE);

    static { NativeItemCatalog.addExtra(Identifier.withDefaultNamespace("slime_ball"), SECTION); }

    public static final HoneyGlueItem HONEY_GLUE = item("honey_glue", HoneyGlueItem::new,
            new Item.Properties().stacksTo(1).durability(100));
    public static final PhysicsStaffItem PHYSICS_STAFF = item("creative_physics_staff", PhysicsStaffItem::new,
            new Item.Properties().rarity(Rarity.EPIC).stacksTo(1));
    public static final PlungerLauncherItem PLUNGER_LAUNCHER = item("plunger_launcher", PlungerLauncherItem::new,
            new Item.Properties().stacksTo(1).durability(200));

    private static <I extends Item> I item(String name, Function<Item.Properties, I> factory, Item.Properties properties) {
        return item(name, factory, properties, Visibility.MAIN);
    }

    private static <I extends Item> I item(String name, Function<Item.Properties, I> factory,
                                          Item.Properties properties, Visibility visibility) {
        final I item = NativeRegistrations.item(id(name), factory, properties);
        NativeItemCatalog.add(item, SECTION, visibility);
        return item;
    }

    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("simulated", path); }
    public static void register() {} // Initializes these real native registrations when invoked by the mod bootstrap.
    private SimItems() {}
}
