package dev.simulated_team.simulated.mixin.schematics;

import com.simibubi.create.content.schematics.SchematicPrinter;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A completed printer has no active cursor to restore when its next blueprint is loaded. */
@Mixin(value = SchematicPrinter.class, remap = false)
public abstract class NativeEmptySchematicResumeMixin {
    @Shadow public abstract void resetSchematic();

    @Inject(method = "fromTag(Lnet/minecraft/nbt/CompoundTag;Z)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void simulated$discardEmptySavedPrinter(CompoundTag tag, boolean clientPacket, CallbackInfo callback) {
        if (clientPacket || tag.contains("Anchor") || tag.contains("CurrentPos")
                || tag.getLongOr("Cursor", -1) != -1 || tag.getIntOr("EntityProgress", -1) != -1
                || !tag.getStringOr("PrintStage", "BLOCKS").equals("BLOCKS")
                || !tag.getStringOr("Fingerprint", "").isEmpty()
                || !tag.getListOrEmpty("DeferredBlocks").isEmpty()) return;
        // Native loadSchematic applies its saved snapshot after setting the deployed anchor.
        // An empty snapshot would overwrite that real anchor with null on the next blueprint.
        resetSchematic();
        callback.cancel();
    }
}
