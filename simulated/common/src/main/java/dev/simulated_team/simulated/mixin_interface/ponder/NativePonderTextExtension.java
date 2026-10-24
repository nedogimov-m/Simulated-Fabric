package dev.simulated_team.simulated.mixin_interface.ponder;

/** Source pointer state copied into one actual Ponder text snapshot. */
public interface NativePonderTextExtension {
    boolean simulated$hidePointer();
    void simulated$setHidePointer(boolean hidden);
}
