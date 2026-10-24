package dev.simulated_team.simulated.mixin.world_presets;

import dev.simulated_team.simulated.index.SimWorldPresets;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native world-type listener already owns fallback/clearing; replace only a selected Simulated description. */
@Mixin(targets="net.minecraft.client.gui.screens.worldselection.CreateWorldScreen$WorldTab")
public abstract class WorldTabMixin {
    @Inject(method="lambda$new$1",at=@At("TAIL"))
    private static void simulated$nativeDescription(CycleButton<WorldCreationUiState.WorldTypeEntry> button,CreateWorldScreen screen,WorldCreationUiState state,CallbackInfo callback) {
        final var holder=state.getWorldType().preset();if(holder==null||holder.unwrapKey().isEmpty())return;
        final var preset=SimWorldPresets.PRESETS.get(holder.unwrapKey().orElseThrow().identifier());
        if(preset!=null&&preset.description()!=null)button.setTooltip(Tooltip.create(preset.description()));
    }
}
