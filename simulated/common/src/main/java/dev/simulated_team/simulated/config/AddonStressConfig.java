package dev.simulated_team.simulated.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.infrastructure.config.NativeConfigBase;
import com.simibubi.create.infrastructure.config.NativeConfigStore;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.DoubleSupplier;

/** Native addon stress definitions; each live supplier reads the authoritative/effective scope store. */
public class AddonStressConfig extends NativeConfigBase {
    private final Map<Identifier, StressValue> impacts = new LinkedHashMap<>();
    private final Map<Identifier, StressValue> capacities = new LinkedHashMap<>();
    private final Map<Identifier, Double> defaultImpacts, defaultCapacities;
    private boolean providersRegistered;

    public AddonStressConfig(String namespace, Map<String, Double> impacts, Map<String, Double> capacities) {
        this.defaultImpacts = this.define(namespace, impacts, this.impacts);
        this.defaultCapacities = this.define(namespace, capacities, this.capacities);
    }

    private Map<Identifier, Double> define(String namespace, Map<String, Double> defaults, Map<Identifier, StressValue> values) {
        final Map<Identifier, Double> result = new LinkedHashMap<>();
        defaults.forEach((path, value) -> {
            final Identifier id = Identifier.fromNamespaceAndPath(namespace, path);
            values.put(id, new StressValue(path, value));
            result.put(id, value);
        });
        return Collections.unmodifiableMap(result);
    }

    @Override
    public String getName() { return "stressValues.v2"; }

    @Override
    public void registerAll(NativeConfigStore.Builder builder) {
        builder.push("impact");
        this.impacts.values().forEach(value -> value.register(builder));
        builder.pop(1);
        builder.push("capacity");
        this.capacities.values().forEach(value -> value.register(builder));
        builder.pop(1);
    }

    public final Map<Identifier, Double> defaultImpacts() { return this.defaultImpacts; }
    public final Map<Identifier, Double> defaultCapacities() { return this.defaultCapacities; }
    public final DoubleSupplier getImpact(Identifier id) {
        final StressValue value = this.impacts.get(id);
        return value == null ? null : value::get;
    }
    public final DoubleSupplier getCapacity(Identifier id) {
        final StressValue value = this.capacities.get(id);
        return value == null ? null : value::get;
    }
    public final DoubleSupplier getImpact(Block block) { return this.getImpact(BuiltInRegistries.BLOCK.getKey(block)); }
    public final DoubleSupplier getCapacity(Block block) { return this.getCapacity(BuiltInRegistries.BLOCK.getKey(block)); }

    public final synchronized void registerProviders() {
        if (this.providersRegistered) return;
        BlockStressValues.IMPACTS.registerProvider(this::getImpact);
        BlockStressValues.CAPACITIES.registerProvider(this::getCapacity);
        this.providersRegistered = true;
    }

    /** Native stress tables use unrestricted doubles, including explicit non-finite string values. */
    private final class StressValue extends CValue<Double> {
        private StressValue(String name, double value) { super(name, value, "Stress Units at one RPM"); }
        @Override
        public Double decode(JsonElement value) {
            if (!value.isJsonPrimitive()) throw new IllegalArgumentException("Expected stress number for " + this.getName());
            if (value.getAsJsonPrimitive().isNumber()) return value.getAsDouble();
            return switch (value.getAsString()) {
                case "NaN" -> Double.NaN;
                case "Infinity" -> Double.POSITIVE_INFINITY;
                case "-Infinity" -> Double.NEGATIVE_INFINITY;
                default -> throw new IllegalArgumentException("Expected stress number for " + this.getName());
            };
        }
        @Override
        public JsonElement encode(Double value) {
            return Double.isFinite(value) ? new JsonPrimitive(value) : new JsonPrimitive(value.toString());
        }
    }
}
