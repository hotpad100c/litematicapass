#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D OpaqueDepth;
uniform sampler2D NoHandDepth;

layout(location = 0) in vec2 texCoord;

const float PUSH = 0.9995;

void main() {
    ivec2 pixelCoords = ivec2(gl_FragCoord.xy);
    float opaque = texelFetch(OpaqueDepth, pixelCoords, 0).r;
    float noHand = texelFetch(NoHandDepth, pixelCoords, 0).r;

    // 光影包将第一人称手使用其自身压缩的深度范围绘制到主缓冲区中，
    // 因此它的深度无法与schematic的深度进行比较。
    // 无论它在哪里改变了深度，它都必须保持在所有事物的最前方。
    // 这里将它初始化为可能的最近深度，这样就不会有任何schematic片段绘制在它上面
    if (opaque != noHand) {
        gl_FragDepth = 1.0;
    } else {
        gl_FragDepth = opaque * PUSH;
    }
}
