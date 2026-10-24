package dev.eriksonn.aeronautics.index;

import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.resources.Identifier;
import java.util.LinkedHashMap;
import java.util.Map;

public class AeroPartialModels {
    private static final Map<Identifier, ExtraModelKey<BlockStateModel>> MODELS = new LinkedHashMap<>();
    private static boolean initialized;
    private static ExtraModelKey<BlockStateModel> model(String path) {
        final var id = Identifier.fromNamespaceAndPath("aeronautics", path);
        final var key = ExtraModelKey.<BlockStateModel>create(id::toString);
        if (MODELS.putIfAbsent(id, key) != null) throw new IllegalStateException("Duplicate partial model " + id);
        return key;
    }
    public static Map<Identifier, ExtraModelKey<BlockStateModel>> allModels() { return Map.copyOf(MODELS); }


	public static final ExtraModelKey<BlockStateModel>
			STEAM_VENT_REDSTONE = block("steam_vent/redstone"),
			STEAM_VENT_BASE = block("steam_vent/steam_base"),
			STEAM_VENT_JET = block("steam_vent/steam_jet"),
			BEARING_PLATE = block("propeller_bearing/bearing_plate"),
			BEARING_PLATE_METAL = block("gyroscopic_propeller_bearing/metal_bearing_plate"),
			GYRO_BEARING_PISTON_HEAD = block("gyroscopic_propeller_bearing/piston_head"),
			GYRO_BEARING_PISTON_POLE = block("gyroscopic_propeller_bearing/piston_pole"),
			HOT_AIR_BURNER_INDICATOR = block("adjustable_burner/redstone_indicator"),
			CANNON_BARREL = block("mounted_potato_cannon/partials/barrel"),
			CANNON_BELLOW = block("mounted_potato_cannon/partials/bellow"),
			CANNON_COG = block("mounted_potato_cannon/partials/cog"),
			ANDESITE_PROPELLER = block("andesite_propeller/propeller"),
			WOODEN_PROPELLER = block("wooden_propeller/propeller"),
			ANDESITE_PROPELLER_REVERSED = block("andesite_propeller/propeller_reversed"),
			WOODEN_PROPELLER_REVERSED = block("wooden_propeller/propeller_reversed"),
			SMART_PROPELLER = block("smart_propeller/propeller"),
			SMART_PROPELLER_REVERSED = block("smart_propeller/propeller_reversed"),
			SMART_PROPELLER_HINGE = block("smart_propeller/hinge");

	private static ExtraModelKey<BlockStateModel> block(final String path) {
		return model("block/" + path);
	}
	private static ExtraModelKey<BlockStateModel> entity(final String path) {
		return model("entity/" + path);
	}
	private static ExtraModelKey<BlockStateModel> item(final String path) {
		return model("item/" + path);
	}

    public static void init() {
        if (initialized) return;
        ModelLoadingPlugin.register(context -> MODELS.forEach((id, key) ->
                context.addModel(key, SimpleUnbakedExtraModel.blockStateModel(id))));
        initialized = true;
    }
}
