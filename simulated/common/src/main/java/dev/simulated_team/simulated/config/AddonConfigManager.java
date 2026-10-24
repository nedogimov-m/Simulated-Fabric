package dev.simulated_team.simulated.config;

import com.google.gson.JsonObject;
import com.simibubi.create.infrastructure.config.ConfigScope;
import com.simibubi.create.infrastructure.config.NativeConfigBase;
import com.simibubi.create.infrastructure.config.NativeConfigStore;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Owns the actual addon JSON files and separates local preferences from authoritative server snapshots. */
public final class AddonConfigManager<S extends NativeConfigBase, C extends NativeConfigBase> {
    private final String modId;
    private final S server;
    private final C client;
    private final AddonStressConfig stress;
    private final NativeConfigStore serverStore, clientStore;
    private Path configDirectory, defaultDirectory, worldFile;
    private JsonObject serverDefaults = new JsonObject();
    private boolean initialized, clientLoaded, serverLoaded;
    private BooleanSupplier serverThread = () -> false;
    private Consumer<Runnable> writer;
    private Consumer<AddonConfigManager<S, C>> changed = ignored -> {};

    public AddonConfigManager(String modId, S server, C client, AddonStressConfig stress) {
        this.modId = Objects.requireNonNull(modId);
        this.server = Objects.requireNonNull(server);
        this.client = Objects.requireNonNull(client);
        this.stress = Objects.requireNonNull(stress);
        this.serverStore = new NativeConfigStore(ConfigScope.SERVER, server);
        this.clientStore = new NativeConfigStore(ConfigScope.CLIENT, client);
        this.writer = operation -> {
            if (this.serverStore.hasRemote()) throw new IllegalStateException("Remote server configuration is read-only");
            operation.run();
        };
        this.serverStore.configure(() -> this.serverThread.getAsBoolean(), operation -> this.writer.accept(operation),
                ignored -> () -> {}, ignored -> {
                    if (this.worldFile == null) this.serverDefaults = this.serverStore.serverDocument();
                    this.changed.accept(this);
                });
    }

    public String modId() { return this.modId; }
    public S server() { return this.server; }
    public C client() { return this.client; }
    public NativeConfigStore serverStore() { return this.serverStore; }
    public NativeConfigStore clientStore() { return this.clientStore; }
    public boolean serverLoaded() { return this.serverLoaded || this.serverStore.hasRemote(); }
    public boolean clientLoaded() { return this.clientLoaded; }

    public void configure(BooleanSupplier serverThread, Consumer<Runnable> writer, Consumer<AddonConfigManager<S, C>> changed) {
        this.serverThread = Objects.requireNonNull(serverThread);
        this.writer = Objects.requireNonNull(writer);
        this.changed = Objects.requireNonNull(changed);
    }

    public synchronized void initialize(Path configDirectory, Path defaultDirectory, boolean loadClient) {
        if (this.initialized) return;
        this.configDirectory = Objects.requireNonNull(configDirectory);
        this.defaultDirectory = Objects.requireNonNull(defaultDirectory);
        this.stress.registerProviders();
        this.serverStore.reload(this.defaultServerFile());
        this.serverLoaded = true;
        if (loadClient) this.reloadClient();
        this.initialized = true;
    }

    private Path defaultServerFile() { return this.defaultDirectory.resolve(this.modId + "-server.json"); }

    public void reloadClient() {
        this.clientStore.reload(this.configDirectory.resolve(this.modId + "-client.json"), new JsonObject(), this.clientLoaded);
        this.clientLoaded = true;
    }

    public void startWorld(Path serverConfigDirectory) {
        if (!this.initialized) throw new IllegalStateException("Configuration lifecycle is not initialized");
        final Path previous = this.worldFile;
        this.worldFile = serverConfigDirectory.resolve(this.modId + "-server.json");
        try {
            this.serverStore.clearRemote();
            this.serverStore.reload(this.worldFile, this.serverDefaults, false);
        } catch (RuntimeException failure) {
            this.worldFile = previous;
            throw failure;
        }
    }

    public void reloadServer() {
        this.writer.accept(() -> this.serverStore.reload(this.worldFile == null ? this.defaultServerFile() : this.worldFile,
                this.worldFile == null ? new JsonObject() : this.serverDefaults, true));
    }

    public void stopWorld() {
        this.worldFile = null;
        this.serverStore.clearRemote();
        this.serverStore.reload(this.defaultServerFile(), new JsonObject(), false);
    }

    public void installRemote(JsonObject document) { this.serverStore.installRemote(document); }
    public void clearRemote() { this.serverStore.clearRemote(); }
}
