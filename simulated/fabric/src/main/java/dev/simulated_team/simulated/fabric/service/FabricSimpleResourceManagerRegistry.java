package dev.simulated_team.simulated.fabric.service;

import dev.simulated_team.simulated.api.SimpleResourceManager;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import java.util.concurrent.atomic.AtomicInteger;

/** Registers every actual codec listener in Fabric's native client resource reload graph. */
public final class FabricSimpleResourceManagerRegistry implements SimpleResourceManager.Registry {
    private static final AtomicInteger NEXT_ID = new AtomicInteger();
    @Override public void registerListener(PreparableReloadListener listener) {
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                Identifier.fromNamespaceAndPath("simulated", "codec_resources/" + NEXT_ID.getAndIncrement()), listener);
    }
}
