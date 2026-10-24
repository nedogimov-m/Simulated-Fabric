package dev.simulated_team.simulated.index;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import foundry.veil.api.client.render.rendertype.VeilRenderType;
import foundry.veil.api.client.render.rendertype.VeilRenderTypeBuilder;
import foundry.veil.api.client.render.rendertype.layer.CullLayer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import com.mojang.renderpearl.api.GpuFormat;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** Native pipelines keep the caller's world, Ponder or GUI render-pass ownership. */
public final class SimRenderTypes {
    // Native BLOCK has no normals; the rope/spring still emit and shade their original normal data.
    private static final VertexFormat LIT_NORMAL_FORMAT = VertexFormat.builder(0)
            .addAttribute("Position", GpuFormat.RGB32_FLOAT)
            .addAttribute("Color", GpuFormat.RGBA8_UNORM)
            .addAttribute("UV0", GpuFormat.RG32_FLOAT)
            .addAttribute("UV2", GpuFormat.RG16_SINT)
            .addAttribute("Normal", GpuFormat.RGBA8_SNORM).build();
    private static final RenderType STAFF_OVERLAY = create("staff_overlay", DefaultVertexFormat.POSITION_COLOR,
            PrimitiveTopology.TRIANGLE_STRIP, false, true, false,
            shader("staff_overlay").transparencyState(Optional.of(BlendFunction.TRANSLUCENT))
                    .depthTestState(CompareOp.ALWAYS_PASS).writeMaskState(true, false));
    private static final RenderType LASER = create("laser", DefaultVertexFormat.POSITION_TEX_COLOR,
            PrimitiveTopology.QUADS, false, true, false,
            shader("laser").transparencyState(Optional.of(BlendFunction.TRANSLUCENT)).cullState(CullLayer.CullFace.NONE));
    private static final RenderType LENS = create("laser_pointer_lens", DefaultVertexFormat.BLOCK,
            PrimitiveTopology.QUADS, true, true, true,
            texture(shader("lens").lightmapState(true), Sheets.BLOCKS_MAPPER.sheet(), true));
    private static final RenderType DIODE = create("redstone_accumulator_diode", DefaultVertexFormat.BLOCK,
            PrimitiveTopology.QUADS, true, false, true,
            texture(shader("diode").lightmapState(true), Sheets.BLOCKS_MAPPER.sheet(), true));
    private static final RenderType LOCK = create("lock", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
            PrimitiveTopology.QUADS, true, false, true,
            texture(shader("lock").lightmapState(true).depthTestState(CompareOp.ALWAYS_PASS)
                    .writeMaskState(true, false).cullState(CullLayer.CullFace.NONE), path("textures/gui/lock.png"), false));
    private static final RenderType ROPE = create("rope", LIT_NORMAL_FORMAT, PrimitiveTopology.QUADS, true, false, false,
            texture(shader("rope").lightmapState(true), path("textures/block/rope_particle.png"), false));
    private static final Function<Identifier, RenderType> SPRING = Util.memoize(texture -> create("spring/" + texture,
            LIT_NORMAL_FORMAT, PrimitiveTopology.QUADS, true, false, true,
            texture(shader("spring").lightmapState(true).overlayState(true), texture, false)));
    private static final RenderType ITEM_GLOWING_SOLID = glowing(false);
    private static final RenderType ITEM_GLOWING_TRANSLUCENT = glowing(true);

    private static VeilRenderTypeBuilder shader(String name) {
        return new VeilRenderTypeBuilder().shaderState(path("native/" + name), false)
                .depthTestState(CompareOp.GREATER_THAN_OR_EQUAL).outputState(null);
    }
    private static VeilRenderTypeBuilder texture(VeilRenderTypeBuilder state, Identifier texture, boolean mipmap) {
        return state.textureState(List.of(new VeilRenderTypeBuilder.Texture(texture, false, mipmap)));
    }
    private static RenderType create(String name, VertexFormat format, PrimitiveTopology topology,
                                     boolean crumbling, boolean sort, boolean outline, VeilRenderTypeBuilder state) {
        return VeilRenderType.create("simulated:" + name, format, topology, RenderType.TRANSIENT_BUFFER_SIZE,
                crumbling, sort, outline, state.create());
    }
    private static RenderType glowing(boolean translucent) {
        final var state = texture(shader("item_glowing").lightmapState(true).overlayState(true), Sheets.BLOCKS_MAPPER.sheet(), false);
        if (translucent) state.transparencyState(Optional.of(BlendFunction.TRANSLUCENT));
        return create("item_glowing_" + (translucent ? "translucent" : "solid"), DefaultVertexFormat.ENTITY,
                PrimitiveTopology.QUADS, true, translucent, true, state);
    }
    public static RenderType staffOverlay() { return STAFF_OVERLAY; }
    public static RenderType laser() { return LASER; }
    public static RenderType lens() { return LENS; }
    public static RenderType diode() { return DIODE; }
    public static RenderType lock() { return LOCK; }
    public static RenderType rope() { return ROPE; }
    public static RenderType spring(Identifier texture) { return SPRING.apply(texture); }
    public static RenderType itemGlowingSolid(boolean shadersActive) { return shadersActive ? Sheets.cutoutBlockItemSheet() : ITEM_GLOWING_SOLID; }
    public static RenderType itemGlowingTranslucent(boolean shadersActive) { return shadersActive ? Sheets.translucentBlockItemSheet() : ITEM_GLOWING_TRANSLUCENT; }
    private static Identifier path(String name) { return Identifier.fromNamespaceAndPath("simulated", name); }
    private SimRenderTypes() {}
}
