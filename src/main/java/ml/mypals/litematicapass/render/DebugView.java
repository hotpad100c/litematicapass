package ml.mypals.litematicapass.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;

import java.util.Locale;
import java.util.Optional;

/**
 * 调试辅助工具：用 schematic 管线所使用的其中一个缓冲区替换当前画面。
 */
public final class DebugView {
	public enum Mode {
		OFF("normal rendering"),
		/** 独立目标的颜色，背景为灰色。 */
		COLOR("color of the independent target"),
		/** 独立目标的深度，该深度最初由场景深度初始化。 */
		SCHEMATIC_DEPTH("depth of the independent target (seeded from the scene depth)"),
		/** 在绘制半透明表面之前捕获的场景深度。 */
		SCENE_DEPTH("scene depth before translucents (the seed source)"),
		/** 最终的主深度，包含半透明表面和手部。 */
		MAIN_DEPTH("final main depth"),
		/** 主深度中仅位于不透明场景前方的半透明表面部分。 */
		TRANSLUCENT_DEPTH("translucent-only depth (main depth vs. depth before translucents)");

		public final String description;

		Mode(String description) {
			this.description = description;
		}

		public String id() {
			return this.name().toLowerCase(Locale.ROOT);
		}
	}

	private static Mode mode = Mode.OFF;
	private static final RenderPipeline[] PIPELINES = new RenderPipeline[Mode.values().length];

	private DebugView() {}

	public static Mode mode() {
		return mode;
	}

	public static void setMode(Mode newMode) {
		mode = newMode;
	}

	private static RenderPipeline pipeline(Mode mode) {
		RenderPipeline pipeline = PIPELINES[mode.ordinal()];
		if (pipeline == null) {
			pipeline = RenderPipelines.register(Gpu.colorOutput(Gpu.screenQuad("pipeline/debug_view/" + mode.id(), "core/debug_view",
					"SchematicColor", "SchematicDepth", "MainDepth", "OpaqueDepth")
				.withShaderDefine("MODE", mode.ordinal()), Optional.empty())
				.build());
			PIPELINES[mode.ordinal()] = pipeline;
		}
		return pipeline;
	}

	static void draw(RenderTarget main, RenderTarget schematic) {
		RenderTarget opaque = SceneDepth.opaque();
		try (RenderPass pass = Gpu.colorPass("litematicapass_debug_view", main)) {
			Gpu.setPipeline(pass, pipeline(mode));
			Gpu.bindColor(pass, "SchematicColor", schematic);
			Gpu.bindDepth(pass, "SchematicDepth", schematic);
			Gpu.bindDepth(pass, "MainDepth", main);
			Gpu.bindDepth(pass, "OpaqueDepth", opaque != null ? opaque : main);
			Gpu.drawScreenQuad(pass);
		}
	}
}
