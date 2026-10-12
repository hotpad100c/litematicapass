#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D OpaqueDepth;
uniform sampler2D NoHandDepth;

const float PUSH = 0.9995;

// 26.2 起深度是反向的（近 = 1，远 = 0）；更早的版本定义 STANDARD_DEPTH（近 = 0，远 = 1）。
// 下面的比较都基于"接近度"：越大越近，远处为 0。
#ifdef STANDARD_DEPTH
float closeness(float depth) { return 1.0 - depth; }
float depthOf(float closeness) { return 1.0 - closeness; }
#else
float closeness(float depth) { return depth; }
float depthOf(float closeness) { return closeness; }
#endif

void main() {
    ivec2 pixelCoords = ivec2(gl_FragCoord.xy);
    float opaque = texelFetch(OpaqueDepth, pixelCoords, 0).r;
    float noHand = texelFetch(NoHandDepth, pixelCoords, 0).r;

    // 光影包将第一人称手使用其自身压缩的深度范围绘制到主缓冲区中，
    // 因此它的深度无法与schematic的深度进行比较。
    // 无论它在哪里改变了深度，它都必须保持在所有事物的最前方。
    // 这里将它初始化为可能的最近深度，这样就不会有任何schematic片段绘制在它上面
    if (opaque != noHand) {
        gl_FragDepth = depthOf(1.0);
    } else {
        gl_FragDepth = depthOf(closeness(opaque) * PUSH);
    }
}
