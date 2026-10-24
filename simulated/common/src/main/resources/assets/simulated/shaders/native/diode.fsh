#version 330
#extension GL_ARB_separate_shader_objects : require
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:fog.glsl>
layout(location=0) in float sphericalDistance;
layout(location=1) in float cylindricalDistance;
layout(location=2) in vec2 texCoord;
layout(location=3) in vec4 vertexColor;
layout(location=4) in vec4 lightColor;
layout(location=5) in float litFraction;
uniform sampler2D Sampler0;
layout(location=0) out vec4 fragColor;
void main() {
    vec4 unlit=texture(Sampler0,texCoord)*lightColor;
    vec4 lit=texture(Sampler0,texCoord+vec2(16,0)/vec2(textureSize(Sampler0,0)));
    vec4 color=mix(lit,unlit,litFraction)*vertexColor*ColorModulator;
    fragColor=apply_fog(color,sphericalDistance,cylindricalDistance,FogEnvironmentalStart,FogEnvironmentalEnd,FogRenderDistanceStart,FogRenderDistanceEnd,FogColor);
}
