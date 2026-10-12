package ml.mypals.litematicapass.render;

import com.mojang.blaze3d.pipeline.RenderTarget;

import java.util.Map;

/**
 * 将我们的 schematic 目标合并到主颜色目标上。
 * 半透明表面（水、玻璃）是在该时间点之后由光影包绘制的，schematic 无法与它们进行混合，因此它只能直接绘制在它们上方。
 * 为了让这画面看起来合理，位于半透明表面背后的 schematic 面会被亮度减半。
 * 将 最终的主深度 与 绘制半透明表面之前的深度 进行比较可以快速确定 半透明部分的深度
 */
final class SchematicComposite {
	private static Gpu.Pipeline pipeline;

	private SchematicComposite() {}

	static void draw(RenderTarget main, RenderTarget schematic) {
		if (pipeline == null) {
			pipeline = Gpu.colorPipeline("pipeline/schematic_composite", "core/schematic_composite", true, Map.of(),
				"SchematicColor", "SchematicDepth", "MainDepth", "OpaqueDepth");
		}
		RenderTarget opaque = SceneDepth.opaque();
		Gpu.draw(pipeline, main,
			Gpu.color("SchematicColor", schematic),
			Gpu.depth("SchematicDepth", schematic),
			Gpu.depth("MainDepth", main),
			Gpu.depth("OpaqueDepth", opaque != null ? opaque : main));
	}
}
