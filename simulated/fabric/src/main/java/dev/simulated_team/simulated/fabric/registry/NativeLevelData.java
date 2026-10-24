package dev.simulated_team.simulated.fabric.registry;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.Nullable;
import java.util.function.Function;

/** Ephemeral level-owned state: releasing a level also releases its data, with no global owner map. */
public final class NativeLevelData<T> {
    private final AttachmentType<T> type;
    private final Function<Level, @Nullable T> factory;
    public NativeLevelData(Identifier id, Function<Level, @Nullable T> factory) {
        this.type=AttachmentRegistry.create(id);this.factory=factory;
    }
    public @Nullable T get(LevelAccessor accessor) {
        if (!(accessor instanceof Level level)) throw new IllegalArgumentException("Expected actual native level owner");
        final var target=(AttachmentTarget)level;
        T value=target.getAttached(this.type);
        if(value!=null)return value;
        synchronized(target) {
            value=target.getAttached(this.type);
            if(value==null) { value=this.factory.apply(level);if(value!=null)target.setAttached(this.type,value); }
            return value;
        }
    }
}
