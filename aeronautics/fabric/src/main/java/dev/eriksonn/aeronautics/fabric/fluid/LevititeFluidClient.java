package dev.eriksonn.aeronautics.fabric.fluid;

import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.eriksonn.aeronautics.util.AeroColors;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/** Native fluid model baking and extracted fog; no graphics state is mutated during extraction. */
public final class LevititeFluidClient {
    public static Map<Fluid, FluidModel> bake(MaterialBaker baker, Map<Fluid, FluidModel> existing) {
        final var models = new IdentityHashMap<Fluid, FluidModel>();
        models.putAll(existing);
        final var model = new FluidModel.Unbaked(
                new Material(Identifier.parse("aeronautics:fluid/levitite_blend_still"), false),
                new Material(Identifier.parse("aeronautics:fluid/levitite_blend_flow"), false), null, null)
                .bake(baker, () -> "aeronautics:levitite_blend");
        models.put(NativeAeroFluids.SOURCE, model);
        models.put(NativeAeroFluids.FLOWING, model);
        return Collections.unmodifiableMap(models);
    }
    public static void fog(Camera camera, ClientLevel level, FogData data) {
        if (!camera.isInitialized()) return;
        final var pos = camera.blockPosition();
        final var fluid = level.getFluidState(pos);
        if (!fluid.getType().isSame(NativeAeroFluids.SOURCE)
                || camera.position().y >= pos.getY() + fluid.getHeight(level, pos)) return;
        final int color = AeroColors.LEVIBLEND_THE_FOG_IS_COMING;
        final float distance = 3 * AllConfigs.client().chocolateTransparencyMultiplier.getF();
        data.color.set(((color >> 16) & 255) / 255f, ((color >> 8) & 255) / 255f, (color & 255) / 255f, 1);
        data.environmentalStart = -8;
        data.environmentalEnd = distance;
        data.skyEnd = distance;
        data.cloudEnd = distance;
    }
    private LevititeFluidClient() {}
}
