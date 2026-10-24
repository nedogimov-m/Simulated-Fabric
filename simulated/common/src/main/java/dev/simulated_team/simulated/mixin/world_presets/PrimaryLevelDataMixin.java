package dev.simulated_team.simulated.mixin.world_presets;

import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import dev.simulated_team.simulated.mixin_interface.PrimaryLevelDataExtension;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.UUID;

@Mixin(PrimaryLevelData.class)
public class PrimaryLevelDataMixin implements PrimaryLevelDataExtension {
    @Unique private static final String simulated$WORLD_PRESET_KEY="simulated:world_preset";
    @Unique private Identifier simulated$worldPresetKey=WorldPresets.NORMAL.identifier();
    @Inject(method="parse",at=@At("RETURN"))
    private static <T> void simulated$parse(Dynamic<T> input,LevelSettings settings,PrimaryLevelData.SpecialWorldProperty special,Lifecycle lifecycle,CallbackInfoReturnable<PrimaryLevelData> result) {
        input.get(simulated$WORLD_PRESET_KEY).read(Identifier.CODEC).result().ifPresent(key->((PrimaryLevelDataExtension)result.getReturnValue()).setPreset(key));
    }
    @Inject(method="setTagData",at=@At("TAIL"))
    private void simulated$savePreset(CompoundTag tag,UUID player,CallbackInfo callback){tag.putString(simulated$WORLD_PRESET_KEY,getPreset().toString());}
    @Override public Identifier getPreset(){return simulated$worldPresetKey;}
    @Override public void setPreset(Identifier preset){simulated$worldPresetKey=preset;}
}
