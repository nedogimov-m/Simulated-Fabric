#version 330
#extension GL_ARB_separate_shader_objects : require
#include <veil:gpu_fullscreen.glsl>
layout(location = 0) out vec2 texCoord;
void main() { texCoord = veil_fullscreen_uv(); gl_Position = vec4(texCoord * 2.0 - 1.0, 0.0, 1.0); }
