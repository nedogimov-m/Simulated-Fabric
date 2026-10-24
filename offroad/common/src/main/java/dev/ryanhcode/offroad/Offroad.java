package dev.ryanhcode.offroad;

import dev.ryanhcode.offroad.events.OffroadCommonEvents;
import dev.ryanhcode.offroad.index.*;
import dev.ryanhcode.offroad.network.OffroadPacketManager;
import dev.ryanhcode.sable.platform.SableEventPlatform;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Offroad {
    public static final String MOD_ID = "offroad";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void init() {
        dev.ryanhcode.offroad.fabric.service.FabricOffroadConfigService.register();
        OffroadDataComponents.init();

        OffroadBlocks.init();
        OffroadBlockEntityTypes.init();
        OffroadEntityTypes.init();
        OffroadItems.init();
        OffroadAdvancements.init();
        dev.ryanhcode.offroad.data.OffroadAdvancementTriggers.register();
        OffroadSoundEvents.init();
        OffroadPacketManager.init();

        OffroadContraptionTypes.init();

        listenCommonEvents();
    }

    private static void listenCommonEvents() {
        SableEventPlatform.INSTANCE.onPhysicsTick(OffroadCommonEvents::physicsTick);
    }

    public static Identifier path(String path) { return Identifier.fromNamespaceAndPath(MOD_ID, path); }
    private Offroad() {}
}
