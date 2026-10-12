package ml.mypals.litematicapass.render;

import com.mojang.blaze3d.pipeline.RenderTarget;

/**
 * 初始化 schematic 目标的深度，该深度决定了什么会遮挡 schematic。
 * - 它从Solid阶段之后的场景深度开始，由 {@link SceneDepth} 在绘制半透明表面之前捕获，不包含半透明深度。
 * - 在绘制第一人称手部的位置，它被初始化为最近距离，以确保手部始终显示在最上方。
 */
final class DepthSeed {
	private static Gpu.Pipeline pipeline;

	private DepthSeed() {}

	static void seed(RenderTarget main, RenderTarget target) {
		if (pipeline == null) {
			pipeline = Gpu.depthPipeline("pipeline/depth_seed", "core/depth_seed", "OpaqueDepth", "NoHandDepth");
		}
		RenderTarget opaque = SceneDepth.opaque();
		RenderTarget noHand = SceneDepth.noHand();
		if (opaque == null) {
			opaque = main;
		}
		if (noHand == null) {
			noHand = opaque;
		}
		Gpu.draw(pipeline, target, Gpu.depth("OpaqueDepth", opaque), Gpu.depth("NoHandDepth", noHand));
	}
}