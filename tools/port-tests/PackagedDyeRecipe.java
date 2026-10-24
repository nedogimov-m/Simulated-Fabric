import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import java.util.List;

/** New actual loaded custom-recipe contract; no mod or game classes are replaced. */
public final class PackagedDyeRecipe {
    public static void runOnServer(MinecraftServer server, List<String> checks) {
        var id=Identifier.parse("simulated:crafting/portable_engine_dyeing");
        var holder=server.getRecipeManager().getRecipes().stream().filter(recipe->recipe.id().identifier().equals(id)).findFirst()
                .orElseThrow(()->new AssertionError("Real portable-engine dyeing recipe was not loaded"));
        if(!(holder.value() instanceof CraftingRecipe recipe))throw new AssertionError("Native dyeing recipe must be a crafting recipe");
        var red=BuiltInRegistries.ITEM.getValue(Identifier.parse("simulated:red_portable_engine"));
        var blue=BuiltInRegistries.ITEM.getValue(Identifier.parse("simulated:blue_portable_engine"));
        var blueDye=BuiltInRegistries.ITEM.getValue(Identifier.parse("minecraft:blue_dye"));
        var redDye=BuiltInRegistries.ITEM.getValue(Identifier.parse("minecraft:red_dye"));
        check(red!=Items.AIR&&blue!=Items.AIR,"Real registered colored portable engine items are required");
        var source=new ItemStack(red,3);source.set(DataComponents.CUSTOM_NAME,Component.literal("Paid engine"));
        var input=CraftingInput.of(2,1,List.of(source,new ItemStack(blueDye)));
        check(recipe.matches(input,server.overworld()),"Actual loaded recipe must accept one engine and one native dye");
        var output=recipe.assemble(input);
        check(output.is(blue)&&output.getCount()==1,"Actual crafting must produce exactly one blue portable engine");
        check(Component.literal("Paid engine").equals(output.get(DataComponents.CUSTOM_NAME)),"Crafting must preserve the real source component patch");
        check(source.getCount()==3&&Component.literal("Paid engine").equals(source.get(DataComponents.CUSTOM_NAME)),"Previewing real recipe must not mutate its input");
        check(!recipe.matches(CraftingInput.of(3,1,List.of(source,new ItemStack(red),new ItemStack(blueDye))),server.overworld()),"Two engine slots must be rejected");
        check(!recipe.matches(CraftingInput.of(3,1,List.of(source,new ItemStack(blueDye),new ItemStack(redDye))),server.overworld()),"Two dye slots must be rejected");
        check(!recipe.matches(CraftingInput.of(2,1,List.of(source,new ItemStack(Items.DIAMOND))),server.overworld()),"Unrelated ingredients must be rejected");
        check(!recipe.matches(CraftingInput.of(1,1,List.of(source)),server.overworld()),"A lone engine must be rejected");
        var codec=Recipe.DIRECT_CODEC;var ops=server.registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
        var json=codec.encodeStart(ops,recipe).getOrThrow();var restored=codec.parse(ops,json).getOrThrow();
        check(restored.getSerializer()==recipe.getSerializer(),"Real registered serializer must roundtrip through native registry-aware recipe codec");
        checks.add("actual_loaded_portable_engine_dye_recipe_color_component_patch_and_rejected_inputs");
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
