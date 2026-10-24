#version 330
#extension GL_ARB_separate_shader_objects : require
layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 2) in vec4 Color;
layout(location = 3) in vec3 Normal;
layout(std140) uniform HeatUniforms {
    mat4 ModelViewMat;
    mat4 ProjMat;
    float Scroll;
    float CutoffY;
    vec4 ColorModulator;
};
layout(location = 0) out vec2 texCoord0;
layout(location = 1) out vec4 vertexColor;
layout(location = 2) out float directionless;
void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    directionless = abs(Normal.y);
    vec2 uv = UV0 + vec2(0.0, floor(Scroll * (1.0 - directionless * 0.4) * 16.0) / 16.0);
    if (directionless != 0.0) uv /= vec2(1.0, 20.0);
    texCoord0 = uv;
    vertexColor = Color * vec4(vec3(1.0), clamp(Position.y - CutoffY, 0.0, 1.0));
}
