package dev.simulated_team.simulated.fabric.tab;

import dev.simulated_team.simulated.fabric.registry.NativeItemCatalog;
import dev.simulated_team.simulated.service.SimTabService;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;

/** The shared native tab consumes the catalog's current ordering and explicit search visibility. */
public final class FabricSimTabService implements SimTabService {
    private CreativeModeTab tab;

    @Override
    public synchronized CreativeModeTab getCreativeTab() {
        if (this.tab == null) {
            this.tab = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
                    Identifier.fromNamespaceAndPath("simulated", "main_tab"),
                    FabricCreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.simulated.group"))
                            .icon(() -> BuiltInRegistries.ITEM.getValue(
                                    Identifier.fromNamespaceAndPath("simulated", "physics_assembler")).getDefaultInstance())
                            .displayItems((parameters, output) -> {
                                for (var entry : NativeItemCatalog.entries()) {
                                    if (entry.visibility() == NativeItemCatalog.Visibility.INVISIBLE) continue;
                                    output.accept(entry.item().getDefaultInstance(), entry.visibility() == NativeItemCatalog.Visibility.SEARCH_ONLY
                                            ? CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY
                                            : CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                                }
                            }).build());
        }
        return this.tab;
    }

    public static void register() { SimTabService.INSTANCE.getCreativeTab(); }
}
