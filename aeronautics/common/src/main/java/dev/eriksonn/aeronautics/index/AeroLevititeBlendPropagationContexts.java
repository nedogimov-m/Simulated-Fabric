package dev.eriksonn.aeronautics.index;

import dev.eriksonn.aeronautics.api.levitite_blend_crystallization.CrystalPropagationContext;
import dev.eriksonn.aeronautics.content.blocks.levitite.LevititeCrystalPropagationContext;
import dev.eriksonn.aeronautics.content.blocks.levitite.LevititeSoulCrystalPropagationContext;
import net.minecraft.core.Registry;

import java.util.function.Supplier;

public class AeroLevititeBlendPropagationContexts {
	public static CrystalPropagationContext
			STANDARD_CONTEXT = create("standard", LevititeCrystalPropagationContext::new),
			SOUL_CONTEXT = create("soul", LevititeSoulCrystalPropagationContext::new);

	public static CrystalPropagationContext create(String id, final Supplier<CrystalPropagationContext> context) {
		return Registry.register(AeroRegistries.LEVITITE_CRYSTAL_PROPAGATION_CONTEXT, dev.eriksonn.aeronautics.Aeronautics.path(id), context.get());
	}

	public static void init() {

	}
}
