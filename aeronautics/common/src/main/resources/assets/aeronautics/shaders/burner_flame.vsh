#version 330
#extension GL_ARB_separate_shader_objects : require
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>
#include <minecraft:fog.glsl>

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 2) in vec2 UV3;
layout(location = 3) in vec4 Color;

layout(location = 0) out vec2 texCoord0;
layout(location = 1) out float sphericalVertexDistance;
layout(location = 2) out float cylindricalVertexDistance;
layout(location = 3) flat out vec3 flameParameters;

void main() {
    vec4 position = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * position;
    sphericalVertexDistance = fog_spherical_distance(position.xyz);
    cylindricalVertexDistance = fog_cylindrical_distance(position.xyz);
    texCoord0 = UV0;
    flameParameters = vec3(UV3, Color.r < 0.5 ? 0.25 : 0.75);
}
