package dev.simulated_team.simulated.mixin.world_presets;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.simulated_team.simulated.index.SimWorldPresets;
import dev.simulated_team.simulated.mixin_interface.PrimaryLevelDataExtension;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.server.RegistryLayer;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.LevelDataAndDimensions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import java.util.Optional;

@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin {
    @Shadow @Final private WorldCreationUiState uiState;
    @WrapMethod(method="createNewWorld")
    private boolean simulated$nativePreset(LayeredRegistryAccess<RegistryLayer> registries,LevelDataAndDimensions.WorldDataAndGenSettings settings,Optional<GameRules> rules,Operation<Boolean> original) {
        final var holder=uiState.getWorldType().preset();
        if(holder!=null&&holder.unwrapKey().isPresent()) {
            final var key=holder.unwrapKey().orElseThrow().identifier();((PrimaryLevelDataExtension)settings.data()).setPreset(key);
            final var preset=SimWorldPresets.PRESETS.get(key);
            if(preset!=null){final var actual=rules.orElseGet(uiState::getGameRules);preset.modifyGameRules(actual);rules=Optional.of(actual);}
        }
        return original.call(registries,settings,rules);
    }
}
