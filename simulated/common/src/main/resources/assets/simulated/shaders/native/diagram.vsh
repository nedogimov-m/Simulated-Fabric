#version 330
#extension GL_ARB_separate_shader_objects : require
#include <veil:gpu_fullscreen.glsl>
layout(location = 0) out vec2 texCoord;
void main() {
    vec2 uv = veil_fullscreen_uv();
    gl_Position = vec4(uv * 2.0 - 1.0, 0.0, 1.0);
    texCoord = vec2(uv.x, 1.0 - uv.y);
}
