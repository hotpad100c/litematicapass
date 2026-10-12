package ml.mypals.litematicapass.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.commands.RenderPassDescriptor;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import ml.mypals.litematicapass.LitematicaPass;
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
			BindGroupLayout layout = BindGroupLayout.builder()
				.withUniform("SchematicColor", UniformType.COMBINED_IMAGE_SAMPLER)
				.withUniform("SchematicDepth", UniformType.COMBINED_IMAGE_SAMPLER)
				.withUniform("MainDepth", UniformType.COMBINED_IMAGE_SAMPLER)
				.withUniform("OpaqueDepth", UniformType.COMBINED_IMAGE_SAMPLER)
				.build();
			pipeline = RenderPipelines.register(RenderPipeline.builder()
				.withLocation(LitematicaPass.id("pipeline/debug_view/" + mode.id()))
				.withVertexShader("core/screenquad")
				.withFragmentShader(LitematicaPass.id("core/debug_view"))
				.withShaderDefine("MODE", mode.ordinal())
				.withBindGroupLayout(layout)
				.withColorTargetState(new ColorTargetState(Optional.empty(), GpuFormat.RGBA8_UNORM, ColorTargetState.WRITE_COLOR))
				.withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
				.build());
			PIPELINES[mode.ordinal()] = pipeline;
		}
		return pipeline;
	}

	static void draw(RenderTarget main, RenderTarget schematic) {
		GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
		GpuTextureView opaque = SceneDepth.opaqueView();
        assert main.getColorTextureView() != null;
        RenderPassDescriptor descriptor = RenderPassDescriptor.builder(() -> "litematicapass_debug_view")
			.withColorAttachment(main.getColorTextureView())
			.withRenderArea(new RenderPass.RenderArea(0, 0, main.width, main.height))
			.build();
		try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(descriptor)) {
			pass.setPipeline(RenderSystem.getCompiledPipeline(pipeline(mode)));
			RenderSystem.bindDefaultUniforms(pass);
			pass.setUniform("SchematicColor", schematic.getColorTextureView(), sampler);
			pass.setUniform("SchematicDepth", schematic.getDepthTextureView(), sampler);
			pass.setUniform("MainDepth", main.getDepthTextureView(), sampler);
			pass.setUniform("OpaqueDepth", opaque != null ? opaque : main.getDepthTextureView(), sampler);
			pass.draw(3, 1, 0, 0);
		}
	}
}
