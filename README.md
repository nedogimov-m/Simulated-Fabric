# Create Aeronautics — Fabric 26.3

The Simulated, Aeronautics and Offroad components are one project and ship in **one installable Aeronautics Bundle JAR**. Component namespaces and Fabric entrypoints remain intact inside the bundle.

## Workspace

Clone the Fabric 26.3 branches into sibling directories:

```sh
git clone -b 26.3 https://github.com/nedogimov-m/imguimc-fabric.git imguimc
git clone -b 26.3 https://github.com/nedogimov-m/Veil-Fabric.git veil
git clone -b 26.3 https://github.com/nedogimov-m/sable-fabric.git sable
git clone -b 26.3 https://github.com/nedogimov-m/Simulated-Fabric.git Aeronautics
```

Use JDK 25. Gradle 9.7 and Fabric Loom 1.18.4 are pinned by the wrapper/build files.

## Build

```sh
(cd ../sable && ./gradlew assemble --no-configuration-cache)
./gradlew assemble --no-configuration-cache
```

The existing Create 26.3 port JAR must be in `../26_3_porting/build/libs/`, or supplied with `-Pcreate_jar=/path/to/create-fabric.jar`. This project never rebuilds Create.

Output: `build/libs/aeronautics-bundle-fabric-26.3-*.jar`. Install the bundle with Create, Sable and Fabric API 0.165.0+26.3. Remove separately installed Simulated/Aeronautics/Offroad JARs first. Sable includes Companion, Veil and ImGuiMC.

## Configuration and validation

All active source sets and services use Fabric. Global, client and per-world configuration uses Fabric lifecycle events; the Sable runtime no longer requires a loader compatibility API.

The Fabric 26.3 build and native integrated-world check passed, including discovery of all three nested components and live physics configuration reload. Earlier flight, interactions and source-conservation checks retain their original scope; source cleanup does not claim a repeat of every gameplay test.

Optional ComputerCraft/JEI/compass APIs are compile-only. Genuine Fabric API references are SHA-pinned; older intermediary descriptors are mapped for compilation, never bundled as runtime mods.

Original work: Simulated Team. Preserve `LICENSE.md` and upstream attribution.
