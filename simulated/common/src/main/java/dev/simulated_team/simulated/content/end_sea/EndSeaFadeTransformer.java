package dev.simulated_team.simulated.content.end_sea;

import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import io.github.ocelot.glslprocessor.api.GlslInjectionPoint;
import io.github.ocelot.glslprocessor.api.GlslParser;
import io.github.ocelot.glslprocessor.api.GlslSyntaxException;
import io.github.ocelot.glslprocessor.api.node.GlslTree;

/** Native moving-block Position is already plot-transformed and camera-relative, before view rotation. */
public final class EndSeaFadeTransformer implements ShaderPreProcessor {
    @Override public void modify(Context context,GlslTree tree)throws GlslSyntaxException{
        if(!context.isVertex())return;
        tree.getBody().add(GlslInjectionPoint.BEFORE_MAIN,GlslParser.parseExpression("""
            layout(std140) uniform SimulatedEndSea {
                float EndSeaCameraToStartY;
                float EndSeaActive;
            };
            """));
        tree.mainFunction().orElseThrow().getBody().add(GlslParser.parseExpression("""
            if (EndSeaActive != 0.0) {
                vertexColor.rgb = mix(vertexColor.rgb, vec3(0.086, 0.078, 0.109) * 2.0,
                    clamp((-pos.y - EndSeaCameraToStartY) / 30.0, 0.0, 1.0));
            }
            """));
    }
}
