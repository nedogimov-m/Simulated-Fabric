#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
layout(std140) uniform HeatUniforms {
    mat4 ModelViewMat;
    mat4 ProjMat;
    float Scroll;
    float CutoffY;
    vec4 ColorModulator;
};
layout(location = 0) in vec2 texCoord0;
layout(location = 1) in vec4 vertexColor;
layout(location = 2) in float directionless;
layout(location = 0) out vec4 fragColor;
void main() {
    vec4 color = (directionless > 0.0 ? texture(Sampler1, texCoord0) : texture(Sampler0, texCoord0)) * vertexColor;
    if (color.a < 0.01) discard;
    fragColor = color * ColorModulator;
}
