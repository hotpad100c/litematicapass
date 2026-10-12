#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D SchematicColor;
uniform sampler2D SchematicDepth;
uniform sampler2D MainDepth;
uniform sampler2D OpaqueDepth;
uniform sampler2D NoHandDepth;

layout(location = 0) out vec4 fragColor;

// 当schematic位于场景的半透明表面后方时，其颜色会乘以该系数。
const float BEHIND_TRANSLUCENT = 0.5;

const float TRANSLUCENT_EXISTS = 1.0005;
const float BEHIND = 0.999;

// 26.2 起深度是反向的（近 = 1，远 = 0）；更早的版本定义 STANDARD_DEPTH（近 = 0，远 = 1）。
// 下面的比较都基于"接近度"：越大越近，远处为 0。
#ifdef STANDARD_DEPTH
float closeness(float depth) { return 1.0 - depth; }
#else
float closeness(float depth) { return depth; }
#endif

void main() {
    ivec2 pixelCoords = ivec2(gl_FragCoord.xy);
    vec4 color = texelFetch(SchematicColor, pixelCoords, 0);
    if (color.a <= 0.0) {
        discard;
    }

    // 第一人称手部所在之处，绘制手部前后的深度不同；手部必须始终在最上方。
    // 深度初始化（depth_seed）也会处理这一点，但它可能早于手部的捕获（Litematica 在手部之前绘制固体地形时）。
    if (texelFetch(OpaqueDepth, pixelCoords, 0).r != texelFetch(NoHandDepth, pixelCoords, 0).r) {
        discard;
    }

    float schematicDepth = closeness(texelFetch(SchematicDepth, pixelCoords, 0).r);
    float mainDepth = closeness(texelFetch(MainDepth, pixelCoords, 0).r);
    float opaqueDepth = closeness(texelFetch(OpaqueDepth, pixelCoords, 0).r);

    // 只有在不透明场景前方绘制了半透明表面的地方，主深度才会比绘制半透明表面之前的深度更近。
    // 如果该表面的距离比 schematic 更近，则说明 schematic 位于其后方。
    bool translucentInFront = mainDepth > opaqueDepth * TRANSLUCENT_EXISTS;
    bool behindTranslucent = translucentInFront && schematicDepth < mainDepth * BEHIND;

    // 颜色是预乘的，因此对其缩放会同时改变亮度和不透明度。
    fragColor = behindTranslucent ? color * BEHIND_TRANSLUCENT : color;
}