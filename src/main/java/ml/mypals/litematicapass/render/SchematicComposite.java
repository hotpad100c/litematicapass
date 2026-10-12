package ml.mypals.litematicapass.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.commands.RenderPassDescriptor;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import ml.mypals.litematicapass.LitematicaPass;
import net.minecraft.client.renderer.RenderPipelines;

import java.util.Optional;

/**
 * 将我们的 schematic 目标合并到主颜色目标上。
 * 半透明表面（水、玻璃）是在该时间点之后由光影包绘制的，schematic 无法与它们进行混合，因此它只能直接绘制在它们上方。
 * 为了让这画面看起来合理，位于半透明表面背后的 schematic 面会被亮度减半。
 * 将 最终的主深度 与 绘制半透明表面之前的深度 进行比较可以快速确定 半透明部分的深度
 */
final class SchematicComposite {
	private static RenderPipeline pipeline;

	private SchematicComposite() {}

	private static RenderPipeline pipeline() {
		if (pipeline == null) {
			BindGroupLayout layout = BindGroupLayout.builder()
				.withUniform("SchematicColor", UniformType.COMBINED_IMAGE_SAMPLER)
				.withUniform("SchematicDepth", UniformType.COMBINED_IMAGE_SAMPLER)
				.withUniform("MainDepth", UniformType.COMBINED_IMAGE_SAMPLER)
				.withUniform("OpaqueDepth", UniformType.COMBINED_IMAGE_SAMPLER)
				.build();
			pipeline = RenderPipelines.register(RenderPipeline.builder()
				.withLocation(LitematicaPass.id("pipeline/schematic_composite"))
				.withVertexShader("core/screenquad")
				.withFragmentShader(LitematicaPass.id("core/schematic_composite"))
				.withBindGroupLayout(layout)
				.withColorTargetState(new ColorTargetState(
					Optional.of(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA), GpuFormat.RGBA8_UNORM, ColorTargetState.WRITE_COLOR))
				.withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
				.build());
		}
		return pipeline;
	}

	static void draw(RenderTarget main, RenderTarget schematic) {
		RenderPipeline pipeline = pipeline();
		GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
        assert main.getColorTextureView() != null;
        RenderPassDescriptor descriptor = RenderPassDescriptor.builder(() -> "litematicapass_schematic_composite")
			.withColorAttachment(main.getColorTextureView())
			.withRenderArea(new RenderPass.RenderArea(0, 0, main.width, main.height))
			.build();
		try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(descriptor)) {
			pass.setPipeline(RenderSystem.getCompiledPipeline(pipeline));
			RenderSystem.bindDefaultUniforms(pass);
			GpuTextureView opaque = SceneDepth.opaqueView();
			pass.setUniform("SchematicColor", schematic.getColorTextureView(), sampler);
			pass.setUniform("SchematicDepth", schematic.getDepthTextureView(), sampler);
			pass.setUniform("MainDepth", main.getDepthTextureView(), sampler);
			pass.setUniform("OpaqueDepth", opaque != null ? opaque : main.getDepthTextureView(), sampler);
			pass.draw(3, 1, 0, 0);
		}
	}
}
