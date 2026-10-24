package dev.simulated_team.simulated.util;

import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkSource;

public class SimLevelUtil {
    public static boolean isAreaActuallyLoaded(final Level level, final BlockPos center, final int range) {
        if (Sable.HELPER.getContaining(level, center) != null) return true;
        final int minY = center.getY() - range;
        final int maxY = center.getY() + range;
        if (maxY < level.getMinY() || minY >= level.getMaxY()) return false;

        // Check the actual loaded chunk owner on both sides without loading chunks.
        final int minChunkX = SectionPos.blockToSectionCoord(center.getX() - range);
        final int maxChunkX = SectionPos.blockToSectionCoord(center.getX() + range);
        final int minChunkZ = SectionPos.blockToSectionCoord(center.getZ() - range);
        final int maxChunkZ = SectionPos.blockToSectionCoord(center.getZ() + range);
        final ChunkSource chunks = level.getChunkSource();
        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                if (!chunks.hasChunk(x, z)) return false;
            }
        }
        return true;
    }
}
