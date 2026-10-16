package ml.mypals.litematicapass.render;

import java.util.Collections;

/**
 * 将我们的 schematic 目标合并到 Minecraft 的帧缓冲（OptiFine 的 final 通道刚写入的着色结果）上。
 * 半透明表面（水、玻璃）在光影包里已经着色进画面，schematic 无法与它们进行混合，因此它只能直接绘制在它们上方。
 * 为了让这画面看起来合理，位于半透明表面背后的 schematic 面会被亮度减半。
 * 将 最终的场景深度 与 绘制半透明表面之前的深度 进行比较可以快速确定 半透明部分的深度。
 */
final class SchematicComposite {
	private static Gpu.Pipeline pipeline;

	private SchematicComposite() {}

	static void draw(Gpu.Output main, Gpu.Target schematic) {
		if (pipeline == null) {
			pipeline = Gpu.colorPipeline("core/schematic_composite", true, Collections.<String, Integer>emptyMap(),
				"SchematicColor", "SchematicDepth", "MainDepth", "OpaqueDepth", "NoHandDepth");
		}
		Gpu.draw(pipeline, main,
			Gpu.sampler("SchematicColor", schematic.color),
			Gpu.sampler("SchematicDepth", schematic.depth),
			Gpu.sampler("MainDepth", SceneDepth.main()),
			Gpu.sampler("OpaqueDepth", SceneDepth.opaque()),
			Gpu.sampler("NoHandDepth", SceneDepth.noHand()));
	}
}
