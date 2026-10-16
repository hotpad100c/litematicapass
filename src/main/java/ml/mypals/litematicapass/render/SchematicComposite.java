package ml.mypals.litematicapass.render;

import java.util.Collections;

/**
 * Composites the schematic onto OptiFine’s final framebuffer, drawing it above already-shaded
 * translucent surfaces while halving the brightness of schematic geometry behind them based on
 * scene-depth comparison.
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
