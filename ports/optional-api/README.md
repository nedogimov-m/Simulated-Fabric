# Genuine optional Minecraft 26.3 compile targets

The Simulated Fabric Gradle module uses these exact upstream compile-only binaries:

| Integration | Maven coordinate | Repository |
| --- | --- | --- |
| JEI common API | `mezz.jei:jei-26.3-common-api:31.16.0.87` | `https://maven.blamejared.com/` |
| JEI Fabric API | `mezz.jei:jei-26.3-fabric-api:31.16.0.87` | `https://maven.blamejared.com/` |
| Nature's Compass | `maven.modrinth:natures-compass:26.3-2.5.1-fabric` | `https://api.modrinth.com/maven` |
| Explorer's Compass | `maven.modrinth:explorers-compass:26.3-2.5.2-fabric` | `https://api.modrinth.com/maven` |

The production build and combined diagnostic read the same local JARs in this directory. `tools/resolve_optional_apis.py`, called before compilation, verifies every SHA256 and downloads missing files from the recorded upstream URLs. This avoids the failing Gradle TLS path to the JEI repository. Exact URLs and hashes are in `artifacts.json`; downloaded JARs are ignored by Git. The full JEI Fabric JAR was inspected only to verify real plugin discovery, not added to the compile classpath. No optional mod is embedded or made a required runtime dependency.
