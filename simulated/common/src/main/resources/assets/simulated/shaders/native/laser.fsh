#version 330
#extension GL_ARB_separate_shader_objects : require
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:fog.glsl>
layout(location=0) in float sphericalDistance;
layout(location=1) in float cylindricalDistance;
layout(location=2) in vec2 lengthData;
layout(location=3) in vec4 vertexColor;
layout(location=0) out vec4 fragColor;
void main() {
    float endTaper=(lengthData.x-1.0)/(lengthData.y-1.0);
    vec4 color=vertexColor;
    color.a*=1.0-max(endTaper,0.0);
    float fade=1.0-total_fog_value(sphericalDistance,cylindricalDistance,
        FogEnvironmentalStart,FogEnvironmentalEnd,FogRenderDistanceStart,FogRenderDistanceEnd);
    fragColor=color*ColorModulator*fade;
}
