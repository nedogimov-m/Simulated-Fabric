package dev.simulated_team.simulated.api;

import com.simibubi.create.content.contraptions.MountedStorageManager;

/** Constructor-time factory only; native storage ownership and codecs remain on the real manager. */
public interface NativeMountedStorageProvider {
    MountedStorageManager simulated$createMountedStorage();
}
