package dev.eriksonn.aeronautics.index;

import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.content.ponder.scenes.*;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

public class AeroPonderScenes {

    public static void register(final PonderSceneRegistrationHelper<Identifier> registry) {
        final PonderSceneRegistrationHelper<net.minecraft.world.level.ItemLike> helper = registry.withKeyFunction(item -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.asItem()));

        helper.forComponents(aeroItemProvider("levitite_blend_bucket"))
                .addStoryBoard("levitite/blend", LevititeScenes::levititeBlend)
                .addStoryBoard("levitite/levitite",(builder,util) -> LevititeScenes.levitite(builder,util,false));
        helper.forComponents(AeroBlocks.LEVITITE)
                .addStoryBoard("levitite/levitite",(builder,util) -> LevititeScenes.levitite(builder,util,false))
                .addStoryBoard("levitite/blend", LevititeScenes::levititeBlend);
        helper.forComponents(AeroBlocks.PEARLESCENT_LEVITITE)
                .addStoryBoard("levitite/levitite",(builder,util) -> LevititeScenes.levitite(builder,util,true))
                .addStoryBoard("levitite/blend", LevititeScenes::levititeBlend);

        helper.forComponents(AeroBlocks.HOT_AIR_BURNER)
                .addStoryBoard("hot_air/adjustable_burner", (scene, util) -> HotAirScenes.hotAirBurner(scene, util, false))
                        .addStoryBoard("airpressure/burner",(builder,util) -> AirPressureScenes.airPressure(builder,util,new AirPressureScenes.Burner()));
        helper.forComponents(AeroBlocks.STEAM_VENT)
                .addStoryBoard("hot_air/steam_vent", (scene, util) -> HotAirScenes.hotAirBurner(scene, util, true))
                .addStoryBoard("airpressure/vent",(builder,util) -> AirPressureScenes.airPressure(builder,util,new AirPressureScenes.Vent()));
        helper.forComponents(AeroBlocks.DYED_ENVELOPE_BLOCKS)
                .addStoryBoard("hot_air/envelope", HotAirScenes::envelope);

        //KINETICS
        helper.forComponents(AeroBlocks.PROPELLER_BEARING)
                .addStoryBoard("propeller_bearing/thrust", (builder,util) -> PropellerScenes.propellerBearingThrust(builder,util,false))
                .addStoryBoard("propeller_bearing/size", PropellerScenes::propellerBearingSize)
                .addStoryBoard("airpressure/propeller", (builder,util) -> AirPressureScenes.airPressure(builder,util,new AirPressureScenes.PropellerBearing()));
        helper.forComponents(AeroBlocks.GYROSCOPIC_PROPELLER_BEARING)
                .addStoryBoard("gyroscopic_propeller_bearing/helicopter", PropellerScenes::gyroBearingStabilize)
                .addStoryBoard("gyroscopic_propeller_bearing/island", PropellerScenes::gyroBearingIsland)
                .addStoryBoard("gyroscopic_propeller_bearing/redstone", PropellerScenes::gyroBearingRedstone)
                .addStoryBoard("airpressure/gyro", (builder,util) -> AirPressureScenes.airPressure(builder,util,new AirPressureScenes.GyroBearing()));
        helper.forComponents(AeroBlocks.MOUNTED_POTATO_CANNON)
                .addStoryBoard("mounted_potato_cannon/intro", MountedPotatoCannonScenes::mountedPotatoCannonIntro);
        helper.forComponents(AeroBlocks.ANDESITE_PROPELLER)
                .addStoryBoard("propeller_bearing/thrust", (builder,util) -> PropellerScenes.propellerBearingThrust(builder,util,true))
                        .addStoryBoard("airpressure/miniprop",(builder,util) -> AirPressureScenes.airPressure(builder,util,new AirPressureScenes.Miniprop()));
        helper.forComponents(AeroBlocks.WOODEN_PROPELLER)
                .addStoryBoard("propeller_bearing/thrust", (builder,util) -> PropellerScenes.propellerBearingThrust(builder,util,true))
                .addStoryBoard("airpressure/miniprop",(builder,util) -> AirPressureScenes.airPressure(builder,util,new AirPressureScenes.Miniprop()));
    }

    private static Item aeroItemProvider(final String id) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Aeronautics.path(id));
    }
}
