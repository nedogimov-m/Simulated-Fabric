package dev.simulated_team.simulated.mixin_interface.ponder;

import dev.simulated_team.simulated.fabric.client.gui.NativePonderShadowSnapshot;

public interface PonderPlatformExtension {
    NativePonderShadowSnapshot simulated$getShadow();
    void simulated$setShadow(NativePonderShadowSnapshot shadow);
}
