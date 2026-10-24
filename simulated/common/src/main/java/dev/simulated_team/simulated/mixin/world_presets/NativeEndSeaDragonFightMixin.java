package dev.simulated_team.simulated.mixin.world_presets;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.simulated_team.simulated.fabric.registry.NativeEndSeaDragonDefaults;
import dev.simulated_team.simulated.index.SimWorldPresets;
import dev.simulated_team.simulated.mixin_interface.PrimaryLevelDataExtension;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerLevel.class)
public abstract class NativeEndSeaDragonFightMixin {
    @WrapOperation(method="<init>",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/storage/SavedDataStorage;computeIfAbsent(Lnet/minecraft/world/level/saveddata/SavedDataType;)Lnet/minecraft/world/level/saveddata/SavedData;"))
    private SavedData simulated$nativeFightDefault(SavedDataStorage storage,SavedDataType<?> type,Operation<SavedData> original) {
        final var level=(ServerLevel)(Object)this;
        if(type.equals(EnderDragonFight.TYPE)&&((PrimaryLevelDataExtension)level.getServer().getWorldData()).getPreset().equals(SimWorldPresets.END_SEA.id()))
            return original.call(storage,NativeEndSeaDragonDefaults.type());
        return original.call(storage,type);
    }
}
