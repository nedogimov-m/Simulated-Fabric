package dev.simulated_team.simulated.fabric.registry;

import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.level.saveddata.SavedDataType;
import java.util.List;
import java.util.Optional;

/** Preserve the source's already-defeated End Sea arena only for absent native fight data. */
public final class NativeEndSeaDragonDefaults {
    public static SavedDataType<EnderDragonFight> type() {
        final var original=EnderDragonFight.TYPE;
        return new SavedDataType<>(original.id(),NativeEndSeaDragonDefaults::create,original.codec(),original.dataFixType());
    }
    public static EnderDragonFight create() {
        return new EnderDragonFight(false,true,true,Optional.empty(),0,Optional.empty(),Optional.empty(),List.of(),List.of());
    }
    private NativeEndSeaDragonDefaults() { }
}
