package dev.simulated_team.simulated.fabric.service;

import dev.simulated_team.simulated.service.SimEntityDataSerialization;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityDataRegistry;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.resources.Identifier;

public final class FabricSimEntityDataSerialization implements SimEntityDataSerialization {
    @Override
    public <A, T extends EntityDataSerializer<A>> void registerDataSerializer(String name, T serializer) {
        FabricEntityDataRegistry.register(Identifier.fromNamespaceAndPath("simulated", name), serializer);
    }
}
