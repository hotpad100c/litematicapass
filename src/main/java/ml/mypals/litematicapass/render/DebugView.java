package ml.mypals.litematicapass.render;

import java.util.Collections;
import java.util.Locale;

public final class DebugView {
	public enum Mode {
		OFF("normal rendering"),
		COLOR("color of the independent target"),
		SCHEMATIC_DEPTH("depth of the independent target (seeded from the scene depth)"),
		SCENE_DEPTH("scene depth before translucents (the seed source)"),
		MAIN_DEPTH("final scene depth"),
		TRANSLUCENT_DEPTH("translucent-only depth (final depth vs. depth before translucents)");

		public final String description;

		Mode(String description) {
			this.description = description;
		}

		public String id() {
			return this.name().toLowerCase(Locale.ROOT);
		}
	}

	private static Mode mode = Mode.OFF;
	private static final Gpu.Pipeline[] PIPELINES = new Gpu.Pipeline[Mode.values().length];

	private DebugView() {}

	public static Mode mode() {
		return mode;
	}

	public static void setMode(Mode newMode) {
		mode = newMode;
	}

	static void draw(Gpu.Output main, Gpu.Target schematic) {
		Gpu.Pipeline pipeline = PIPELINES[mode.ordinal()];
		if (pipeline == null) {
			pipeline = Gpu.colorPipeline("core/debug_view", false, Collections.singletonMap("MODE", mode.ordinal()),
				"SchematicColor", "SchematicDepth", "MainDepth", "OpaqueDepth");
			PIPELINES[mode.ordinal()] = pipeline;
		}
		Gpu.draw(pipeline, main,
			Gpu.sampler("SchematicColor", schematic.color),
			Gpu.sampler("SchematicDepth", schematic.depth),
			Gpu.sampler("MainDepth", SceneDepth.main()),
			Gpu.sampler("OpaqueDepth", SceneDepth.opaque()));
	}
}
