package dev.simulated_team.simulated.fabric.client;

import dev.simulated_team.simulated.content.items.plunger_launcher.PlungerLauncherItemRenderer;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffItemRenderer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.resources.Identifier;

/** Normal Fabric model-loader hooks; no legacy custom-item renderer or deferred live item owner. */
public final class NativeSimAnimatedItems {
    private static boolean registered;
    public static void register(){if(registered)return;registered=true;
        PlungerLauncherItemRenderer.INSTANCE.register(Identifier.parse("simulated:plunger_launcher"));
        PhysicsStaffItemRenderer.INSTANCE.register(Identifier.parse("simulated:creative_physics_staff"));
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->NativeItemFocus.clear());
    }
    private NativeSimAnimatedItems(){ }
}
