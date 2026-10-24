#version 330
#extension GL_ARB_separate_shader_objects : require
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>
#include <minecraft:fog.glsl>
layout(location=0) in vec3 Position;
layout(location=1) in vec2 UV0;
layout(location=2) in vec4 Color;
layout(location=0) out float sphericalDistance;
layout(location=1) out float cylindricalDistance;
layout(location=2) out vec2 lengthData;
layout(location=3) out vec4 vertexColor;
void main() {
    vec4 view=ModelViewMat*vec4(Position,1);
    gl_Position=ProjMat*view;
    sphericalDistance=length(view.xyz);
    cylindricalDistance=fog_cylindrical_distance(view.xyz);
    lengthData=UV0; vertexColor=Color;
}
