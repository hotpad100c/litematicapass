#version 330

uniform sampler2D OpaqueDepth;
uniform sampler2D NoHandDepth;
uniform vec2 OutputSize;

const float PUSH = 0.9995;

// 1.12 的深度是标准的（近 = 0，远 = 1，定义了 STANDARD_DEPTH）；下面的比较都基于"接近度"：越大越近，远处为 0。
#ifdef STANDARD_DEPTH
float closeness(float depth) { return 1.0 - depth; }
float depthOf(float closeness) { return 1.0 - closeness; }
#else
float closeness(float depth) { return depth; }
float depthOf(float closeness) { return closeness; }
#endif

// 各纹理的尺寸可能与输出不同（OptiFine 按自己的渲染分辨率绘制世界），按比例取对应位置的像素。
ivec2 at(sampler2D s) {
    return ivec2(gl_FragCoord.xy * vec2(textureSize(s, 0)) / OutputSize);
}

void main() {
    float opaque = texelFetch(OpaqueDepth, at(OpaqueDepth), 0).r;
    float noHand = texelFetch(NoHandDepth, at(NoHandDepth), 0).r;

    // 光影包将第一人称手使用其自身压缩的深度范围绘制到场景中，
    // 因此它的深度无法与schematic的深度进行比较。
    // 无论它在哪里改变了深度，它都必须保持在所有事物的最前方。
    // 这里将它初始化为可能的最近深度，这样就不会有任何schematic片段绘制在它上面
    if (opaque != noHand) {
        gl_FragDepth = depthOf(1.0);
    } else {
        gl_FragDepth = depthOf(closeness(opaque) * PUSH);
    }
}
