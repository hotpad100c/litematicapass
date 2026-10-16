#version 330

uniform sampler2D SchematicColor;
uniform sampler2D SchematicDepth;
uniform sampler2D MainDepth;
uniform sampler2D OpaqueDepth;
uniform sampler2D NoHandDepth;
uniform vec2 OutputSize;

layout(location = 0) out vec4 fragColor;

// 当schematic位于场景的半透明表面后方时，其颜色会乘以该系数。
const float BEHIND_TRANSLUCENT = 0.5;

const float TRANSLUCENT_EXISTS = 1.0005;
const float BEHIND = 0.999;

// 1.12 的深度是标准的（近 = 0，远 = 1，定义了 STANDARD_DEPTH）；下面的比较都基于"接近度"：越大越近，远处为 0。
#ifdef STANDARD_DEPTH
float closeness(float depth) { return 1.0 - depth; }
#else
float closeness(float depth) { return depth; }
#endif

// 各纹理的尺寸可能与输出不同（OptiFine 按自己的渲染分辨率绘制世界，结果被缩放到 Minecraft 的帧缓冲），按比例取对应位置的像素。
ivec2 at(sampler2D s) {
    return ivec2(gl_FragCoord.xy * vec2(textureSize(s, 0)) / OutputSize);
}

void main() {
    vec4 color = texelFetch(SchematicColor, at(SchematicColor), 0);
    if (color.a <= 0.0) {
        discard;
    }

    float opaqueRaw = texelFetch(OpaqueDepth, at(OpaqueDepth), 0).r;

    // 第一人称手部所在之处，绘制手部前后的深度不同；手部必须始终在最上方。
    // 深度初始化（depth_seed）也会处理这一点，但它可能早于手部的捕获（Litematica 在手部之前绘制固体地形时）。
    if (opaqueRaw != texelFetch(NoHandDepth, at(NoHandDepth), 0).r) {
        discard;
    }

    float schematicDepth = closeness(texelFetch(SchematicDepth, at(SchematicDepth), 0).r);
    float mainDepth = closeness(texelFetch(MainDepth, at(MainDepth), 0).r);
    float opaqueDepth = closeness(opaqueRaw);

    // 只有在不透明场景前方绘制了半透明表面的地方，最终深度才会比绘制半透明表面之前的深度更近。
    // 如果该表面的距离比 schematic 更近，则说明 schematic 位于其后方。
    bool translucentInFront = mainDepth > opaqueDepth * TRANSLUCENT_EXISTS;
    bool behindTranslucent = translucentInFront && schematicDepth < mainDepth * BEHIND;

    // 颜色是预乘的，因此对其缩放会同时改变亮度和不透明度。
    fragColor = behindTranslucent ? color * BEHIND_TRANSLUCENT : color;
}
