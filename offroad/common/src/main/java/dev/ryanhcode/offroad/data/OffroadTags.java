package dev.ryanhcode.offroad.data;

import dev.ryanhcode.offroad.Offroad;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import dev.simulated_team.simulated.fabric.registry.NativeTagDefinitions;
import net.minecraft.tags.TagFile;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class OffroadTags {
    private static TagKey<net.minecraft.world.item.Item> commonItemTag(String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", path));
    }
    private static TagKey<Block> commonBlockTag(String path) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", path));
    }

    public static class Blocks {

        public static final TagKey<Block> BOREHEAD_EFFECTIVE = create("borehead_effective");
        public static final TagKey<Block> BOREHEAD_SUPER_EFFECTIVE = create("borehead_super_effective");

        private static TagKey<Block> create(final String path) {
            return TagKey.create(Registries.BLOCK, Offroad.path(path));
        }

        private static TagKey<Block> create(final String namespace, final String path) {
            return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(namespace, path));
        }

        public static java.util.Map<Identifier, TagFile> blockDefinitions() {
            final NativeTagDefinitions<Block> prov = new NativeTagDefinitions<>(BuiltInRegistries.BLOCK);
            prov.tag(BOREHEAD_EFFECTIVE)
                    .addTag(commonBlockTag("ores"));
            prov.tag(BOREHEAD_SUPER_EFFECTIVE)
                    .add(net.minecraft.world.level.block.Blocks.ANCIENT_DEBRIS);

            return prov.definitions();
        }
    }
}
