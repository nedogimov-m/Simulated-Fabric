package dev.ryanhcode.offroad.index;

import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.resources.Identifier;
import java.util.LinkedHashMap;
import java.util.Map;

public class OffroadPartialModels {
    private static final Map<Identifier, ExtraModelKey<BlockStateModel>> MODELS = new LinkedHashMap<>();
    private static boolean initialized;
    private static ExtraModelKey<BlockStateModel> model(String path) {
        final var id = Identifier.fromNamespaceAndPath("offroad", path);
        final var key = ExtraModelKey.<BlockStateModel>create(id::toString);
        if (MODELS.putIfAbsent(id, key) != null) throw new IllegalStateException("Duplicate partial model " + id);
        return key;
    }
    public static Map<Identifier, ExtraModelKey<BlockStateModel>> allModels() { return Map.copyOf(MODELS); }


	public static final ExtraModelKey<BlockStateModel>
			DIODE_LEFT = block("wheel_mount/diode_left"),
			DIODE_RIGHT = block("wheel_mount/diode_right"),
			TELE_OUTER = block("wheel_mount/tele_outer"),
			TELE_INNER = block("wheel_mount/tele_inner"),
			TELE_MOUNT = block("wheel_mount/mount"),
			SPRING_UPPER = block("wheel_mount/spring_upper"),
			SPRING_MIDDLE = block("wheel_mount/spring_middle"),
			SPRING_LOWER = block("wheel_mount/spring_lower"),
			ROCK_CUTTING_WHEEL_WHEEL = block("rockcutting_wheel/wheel");

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
