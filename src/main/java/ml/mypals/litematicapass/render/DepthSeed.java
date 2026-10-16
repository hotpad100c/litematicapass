package ml.mypals.litematicapass.render;

/**
 * 初始化 schematic 目标的深度，该深度决定了什么会遮挡 schematic。
 * - 它从此刻的场景深度开始（不包含半透明深度；绘制半透明之前已捕获时用 {@link SceneDepth} 的副本，否则用 OptiFine 当前的深度）。
 * - 在绘制第一人称手部的位置，它被初始化为最近距离，以确保手部始终显示在最上方。
 */
final class DepthSeed {
	private static Gpu.Pipeline pipeline;

	private DepthSeed() {}

	static void seed(Gpu.Target target) {
		if (pipeline == null) {
			pipeline = Gpu.depthPipeline("core/depth_seed", "OpaqueDepth", "NoHandDepth");
		}
		Gpu.draw(pipeline, Gpu.Output.of(target),
			Gpu.sampler("OpaqueDepth", SceneDepth.opaque()),
			Gpu.sampler("NoHandDepth", SceneDepth.noHand()));
	}
}
