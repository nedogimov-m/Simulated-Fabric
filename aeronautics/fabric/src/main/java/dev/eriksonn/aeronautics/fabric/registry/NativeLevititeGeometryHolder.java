package dev.eriksonn.aeronautics.fabric.registry;
import org.jetbrains.annotations.Nullable;
/** Extra immutable geometry follows the actual native compilation result and published section owner. */
public interface NativeLevititeGeometryHolder {
    @Nullable NativeLevititeMesh aeronautics$levititeMesh();
    void aeronautics$levititeMesh(@Nullable NativeLevititeMesh mesh);
}
