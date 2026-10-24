package dev.simulated_team.simulated.fabric.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import foundry.veil.api.client.render.rendertype.VeilRenderTypeBuilder;
import foundry.veil.mixin.rendertype.accessor.RenderTypeAccessor;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import java.util.EnumMap;
import java.util.Map;

/** Native copies of the real moving-block setups, with distinct identities for plot-only shader bindings. */
public final class NativeEndSeaPlotTypes {
    private static final Map<ChunkSectionLayer,RenderType> TYPES=new EnumMap<>(ChunkSectionLayer.class);
    public static RenderType type(ChunkSectionLayer layer){return TYPES.computeIfAbsent(layer,key->{
        RenderType original=switch(key){case SOLID->RenderTypes.solidMovingBlock();case CUTOUT->RenderTypes.cutoutMovingBlock();case TRANSLUCENT->RenderTypes.translucentMovingBlock();};
        var state=VeilRenderTypeBuilder.from(((RenderTypeAccessor)(Object)original).veil$getState()).create();
        var colors=original.pipeline().getColorTargetStates().stream().map(value->value==null?null:value.format()).toList();
        var pipeline=VeilRenderTypeBuilder.pipeline(path("pipeline/end_sea_plot_"+key.name().toLowerCase(java.util.Locale.ROOT)),state,
                DefaultVertexFormat.BLOCK,PrimitiveTopology.QUADS,colors,GpuFormat.D32_FLOAT);
        var setup=VeilRenderTypeBuilder.setup(pipeline,state,true,key==ChunkSectionLayer.TRANSLUCENT,true);
        return RenderTypeAccessor.veil$create("simulated_end_sea_plot_"+key,setup);
    });}
    private static Identifier path(String name){return Identifier.fromNamespaceAndPath("simulated",name);}
    private NativeEndSeaPlotTypes(){}
}
