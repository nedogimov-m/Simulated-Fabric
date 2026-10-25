package port.observation;

import com.simibubi.create.content.equipment.EquipmentItemRenderer;
import java.lang.reflect.Field;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.world.item.*;

/** Observes the real resolver used by the visible native inventory; never replaces a model. */
public final class GameplayItems {
    public static final Map<Item, Row> ROWS = new IdentityHashMap<>();
    public static Set<Item> active = Set.of();
    private static final Field LAYERS = field(ItemStackRenderState.class, "layers");
    private static final Field COUNT = field(ItemStackRenderState.class, "activeLayerCount");
    private static final Field QUADS = field(ItemStackRenderState.LayerRenderState.class, "quads");
    private static final Field SPECIAL = field(ItemStackRenderState.LayerRenderState.class, "specialRenderer");
    private static final Field ARGUMENT = field(ItemStackRenderState.LayerRenderState.class, "argumentForSpecialRendering");

    public static final class Row {
        public String id, itemClass, blockClass;
        public List<String> useHooks;
        public int observations, faces;
        public boolean nonempty;
        public final Set<String> sprites = new TreeSet<>(), specialRenderers = new TreeSet<>(), issues = new TreeSet<>();
        public final Set<String> atlasBoundarySamples = new TreeSet<>();
        public final Map<String, String> names = new TreeMap<>();
    }

    public static void observe(ItemStackRenderState state, ItemStack stack, ItemDisplayContext context) {
        if (context != ItemDisplayContext.GUI || !active.contains(stack.getItem())) return;
        var row = ROWS.get(stack.getItem());
        row.observations++;
        row.nonempty |= !state.isEmpty();
        try {
            var layers = (Object[]) LAYERS.get(state);
            for (int index = 0; index < COUNT.getInt(state); index++) {
                var layer = layers[index];
                scan(QUADS.get(layer), row, Collections.newSetFromMap(new IdentityHashMap<>()), 0);
                var mesh = ((net.fabricmc.fabric.impl.client.renderer.LayerRenderStateExtension) layer).fabric_getMutableMesh();
                if (mesh != null) mesh.forEach(quad -> {
                    row.faces++;
                    var atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(quad.atlas().getId());
                    var sprite = ((net.fabricmc.fabric.api.client.renderer.v1.sprite.FabricTextureAtlas) atlas).spriteFinder().find(quad);
                    if (sprite == null || sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
                        var missing = atlas.getSprite(MissingTextureAtlasSprite.getLocation());
                        boolean samplesMissing = true;
                        for (int v = 0; v < 4; v++) samplesMissing &= quad.u(v) >= missing.getU0() && quad.u(v) <= missing.getU1()
                                && quad.v(v) >= missing.getV0() && quad.v(v) <= missing.getV1();
                        // A SpriteFinder boundary-cell fallback alone does not mean the GPU samples missingno.
                        String detail = "atlas=" + quad.atlas().getId() + ", uv=" + quad.u(0) + "," + quad.v(0);
                        if (samplesMissing) row.issues.add("Missing mesh texture: " + detail);
                        else row.atlasBoundarySamples.add(detail);
                    } else row.sprites.add(sprite.contents().name().toString());
                    for (int v = 0; v < 4; v++) if (!Float.isFinite(quad.x(v) + quad.y(v) + quad.z(v) + quad.u(v) + quad.v(v)))
                        row.issues.add("Nonfinite mesh vertex/UV");
                });
                var renderer = SPECIAL.get(layer);
                if (renderer != null) {
                    row.specialRenderers.add(renderer.getClass().getName());
                    scan(ARGUMENT.get(layer), row, Collections.newSetFromMap(new IdentityHashMap<>()), 0);
                }
            }
        } catch (ReflectiveOperationException failure) { throw new AssertionError("Cannot inspect actual native item layers", failure); }
    }

    private static void scan(Object value, Row row, Set<Object> seen, int depth) throws ReflectiveOperationException {
        if (value == null || depth > 10 || !seen.add(value)) return;
        if (value instanceof ItemQuads quads) {
            for (var quad : quads.all()) scan(quad, row, seen, depth + 1);
        } else if (value instanceof BakedQuad quad) {
            row.faces++;
            var sprite = quad.materialInfo().sprite().contents().name();
            row.sprites.add(sprite.toString());
            if (sprite.equals(MissingTextureAtlasSprite.getLocation())) row.issues.add("Missing face texture");
            for (int v = 0; v < 4; v++) {
                var point = quad.position(v);
                if (!Float.isFinite(point.x() + point.y() + point.z())) row.issues.add("Nonfinite face vertex");
            }
        } else if (value instanceof Collection<?> values) {
            for (var child : values) scan(child, row, seen, depth + 1);
        } else if (value instanceof org.joml.Matrix4fc matrix) {
            if (!matrix.isFinite()) row.issues.add("Nonfinite item transform");
        } else if (value instanceof EquipmentItemRenderer.State equipment) {
            scan(equipment.draws, row, seen, depth + 1);
        } else if (value.getClass().isRecord() && (value.getClass().getName().startsWith("com.simibubi.create.")
                || value.getClass().getName().startsWith("dev.simulated_team.") || value.getClass().getName().startsWith("dev.eriksonn.")
                || value.getClass().getName().startsWith("dev.ryanhcode.offroad."))) {
            for (var component : value.getClass().getRecordComponents()) {
                var getter = component.getAccessor(); getter.setAccessible(true);
                scan(getter.invoke(value), row, seen, depth + 1);
            }
        }
    }

    private static Field field(Class<?> owner, String name) {
        try { var result = owner.getDeclaredField(name); result.setAccessible(true); return result; }
        catch (ReflectiveOperationException failure) { throw new ExceptionInInitializerError(failure); }
    }
    private GameplayItems() {}
}
