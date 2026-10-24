package dev.simulated_team.simulated.data.advancements;

import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/** Native player advancement storage owns listeners; Sim supplies the action-specific condition. */
public abstract class SimulatedCriterionTriggerBase<T extends SimulatedCriterionTriggerBase.Instance> extends SimpleCriterionTrigger<T> {
    private final Identifier id;
    public SimulatedCriterionTriggerBase(Identifier id) { this.id = id; }
    public Identifier getId() { return this.id; }
    protected void trigger(ServerPlayer player, @Nullable List<Supplier<Object>> suppliers) {
        super.trigger(player, instance -> instance.test(suppliers));
    }
    public abstract static class Instance implements SimpleCriterionTrigger.SimpleInstance {
        private final Identifier id;
        public Instance(Identifier id) { this.id = id; }
        public Identifier getId() { return this.id; }
        @Override public Optional<Holder<LootItemCondition>> player() { return Optional.empty(); }
        protected abstract boolean test(@Nullable List<Supplier<Object>> suppliers);
    }
}
