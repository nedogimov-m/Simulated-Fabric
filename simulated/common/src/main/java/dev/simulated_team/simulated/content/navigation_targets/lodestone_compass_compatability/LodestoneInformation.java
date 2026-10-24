package dev.simulated_team.simulated.content.navigation_targets.lodestone_compass_compatability;

import net.minecraft.nbt.CompoundTag;
import org.joml.Vector3d;

import java.util.UUID;

public record LodestoneInformation(UUID id, Vector3d projectedPos) {
	public CompoundTag saveAsCompound() {
		final CompoundTag data = new CompoundTag();
		data.put("trackerID", net.minecraft.core.UUIDUtil.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, this.id).getOrThrow());

		return data;
	}

	public static LodestoneInformation loadFromCompound(final CompoundTag tag) {
		return new LodestoneInformation(net.minecraft.core.UUIDUtil.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE,
				tag.get("trackerID")).getOrThrow(), new Vector3d());
	}
}
