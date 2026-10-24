package dev.simulated_team.simulated.mixin.create_assembly;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.MountedStorageManager;
import dev.simulated_team.simulated.api.NativeMountedStorageProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import java.util.Objects;

@Mixin(value=Contraption.class,remap=false)
public abstract class NativeMountedStorageFactoryMixin {
    // The no-arg constructor delegates with this(new Manager()) before Object.<init>.
    // Select the final-field initializer in the initialized argument constructor instead.
    @ModifyArg(method="<init>(Lcom/simibubi/create/content/contraptions/MountedStorageManager;)V",at=@At(value="INVOKE",target="Ljava/util/Objects;requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;"),index=0,require=1,expect=1)
    private Object simulated$addonStorage(Object original) {
        return this instanceof NativeMountedStorageProvider provider
                ? Objects.requireNonNull(provider.simulated$createMountedStorage(),"Addon mounted storage") : original;
    }
}
