package dev.eriksonn.aeronautics.mixin.custom_situational_music;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.eriksonn.aeronautics.api.CustomSituationalMusic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import org.jetbrains.annotations.Nullable;
import java.util.Optional;

@Mixin(Minecraft.class)
public class MinecraftMixin {

	@Shadow @Nullable public LocalPlayer player;

	@Shadow @Nullable public ClientLevel level;

	@ModifyExpressionValue(method = "getSituationalMusic", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/attribute/BackgroundMusic;select(ZZ)Ljava/util/Optional;"))
	private Optional<Music> aeronautics$getSituationalMusic(Optional<Music> original) {
		if (this.level == null || this.player == null || original.filter(Musics.GAME::equals).isEmpty()) {
			return original;
		}
		Music customMusic = CustomSituationalMusic.getSituationalMusic(this.level, this.player);
		return customMusic != null ? Optional.of(customMusic) : original;
	}
}
