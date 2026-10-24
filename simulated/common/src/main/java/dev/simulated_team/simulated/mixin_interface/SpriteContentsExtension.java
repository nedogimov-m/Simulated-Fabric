package dev.simulated_team.simulated.mixin_interface;

/** Borrowed native atlas animation state; replaced by its atlas owner on reload. */
public interface SpriteContentsExtension {
    TickerExtension simulated$getTicker();
    void simulated$setTicker(TickerExtension state);
}
