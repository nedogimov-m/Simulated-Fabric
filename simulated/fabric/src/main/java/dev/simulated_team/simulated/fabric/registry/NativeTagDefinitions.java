package dev.simulated_team.simulated.fabric.registry;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagFile;
import net.minecraft.tags.TagKey;
import java.util.LinkedHashMap;
import java.util.Map;

/** Typed source tag definitions backed by the native TagBuilder/TagFile schema. */
public final class NativeTagDefinitions<T> {
    private final Registry<T> registry;
    private final Map<Identifier, TagBuilder> tags = new LinkedHashMap<>();
    public NativeTagDefinitions(Registry<T> registry) { this.registry = registry; }
    public Builder tag(TagKey<T> tag) { return new Builder(this.tags.computeIfAbsent(tag.location(), id -> TagBuilder.create())); }
    public Map<Identifier, TagFile> definitions() {
        final Map<Identifier, TagFile> result = new LinkedHashMap<>();
        this.tags.forEach((id, builder) -> result.put(id, new TagFile(builder.build(), builder.shouldReplace())));
        return java.util.Collections.unmodifiableMap(result);
    }
    public final class Builder {
        private final TagBuilder builder;
        private Builder(TagBuilder builder) { this.builder = builder; }
        @SafeVarargs public final Builder add(T... values) {
            for (T value : values) this.builder.addElement(java.util.Objects.requireNonNull(registry.getKey(value)));
            return this;
        }
        public Builder addTag(TagKey<T> tag) { this.builder.addTag(tag.location()); return this; }
        public Builder addOptional(Identifier id) { this.builder.addOptionalElement(id); return this; }
    }
}
