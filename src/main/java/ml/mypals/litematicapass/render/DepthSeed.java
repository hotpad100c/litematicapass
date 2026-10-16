package ml.mypals.litematicapass.render;

/**
 * Initializes the schematic depth buffer from the opaque scene depth while excluding translucent geometry,
 * with first-person hand depth forced to the nearest value so it always renders on top of the schematic.
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
