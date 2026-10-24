package dev.simulated_team.simulated.fabric.registry;

import com.mojang.serialization.MapCodec;
import dev.simulated_team.simulated.content.blocks.portable_engine.PortableEngineBlock;
import dev.simulated_team.simulated.index.SimBlocks;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Real native custom crafting recipe: one engine and one dye, retaining its component patch. */
public final class NativePortableEngineDyeingRecipe extends CustomRecipe {
    private static final NativePortableEngineDyeingRecipe INSTANCE = new NativePortableEngineDyeingRecipe();
    public static final RecipeSerializer<NativePortableEngineDyeingRecipe> SERIALIZER = new RecipeSerializer<>(
            MapCodec.unit(INSTANCE), StreamCodec.unit(INSTANCE));

    public static void register() {
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Identifier.parse("simulated:portable_engine_dyeing"), SERIALIZER);
    }
    @Override public boolean matches(CraftingInput input, Level level) {
        int engines = 0, dyes = 0;
        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) continue;
            if (Block.byItem(stack.getItem()) instanceof PortableEngineBlock) engines++;
            else if (stack.has(DataComponents.DYE)) dyes++;
            else return false;
            if (engines > 1 || dyes > 1) return false;
        }
        return engines == 1 && dyes == 1;
    }
    @Override public ItemStack assemble(CraftingInput input) {
        if (!matches(input, null)) return ItemStack.EMPTY;
        ItemStack engine = ItemStack.EMPTY;
        DyeColor color = DyeColor.RED;
        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) continue;
            if (Block.byItem(stack.getItem()) instanceof PortableEngineBlock) engine = stack;
            else color = stack.get(DataComponents.DYE);
        }
        ItemStack result = new ItemStack(SimBlocks.PORTABLE_ENGINES.get(color));
        result.applyComponents(engine.getComponentsPatch());
        return result;
    }
    @Override public CraftingBookCategory category() { return CraftingBookCategory.MISC; }
    @Override public RecipeSerializer<NativePortableEngineDyeingRecipe> getSerializer() { return SERIALIZER; }
}
