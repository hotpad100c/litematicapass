package ml.mypals.litematicapass.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.commands.RenderPassDescriptor;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import ml.mypals.litematicapass.LitematicaPass;
import net.minecraft.client.renderer.RenderPipelines;

import java.util.OptionalDouble;

/**
 * 初始化 schematic 目标的深度，该深度决定了什么会遮挡 schematic。
 * - 它从Solid阶段之后的场景深度开始，由 {@link SceneDepth} 在绘制半透明表面之前捕获，不包含半透明深度。
 * - 在绘制第一人称手部的位置，它被初始化为最近距离，以确保手部始终显示在最上方。
 */
final class DepthSeed {

	private static RenderPipeline pipeline;

	private static RenderPipeline pipeline() {
		if (pipeline == null) {
			BindGroupLayout layout = BindGroupLayout.builder()
				.withUniform("OpaqueDepth", UniformType.COMBINED_IMAGE_SAMPLER)
				.withUniform("NoHandDepth", UniformType.COMBINED_IMAGE_SAMPLER)
				.build();
			pipeline = RenderPipelines.register(RenderPipeline.builder()
				.withLocation(LitematicaPass.id("pipeline/depth_seed"))
				.withVertexShader("core/screenquad")
				.withFragmentShader(LitematicaPass.id("core/depth_seed"))
				.withBindGroupLayout(layout)
				.withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
				.withDepthStencilState(DepthStencilState.DEFAULT)
				.build());
		}
		return pipeline;
	}

	static void seed(RenderTarget main, RenderTarget target) {
		GpuTextureView opaque = SceneDepth.opaqueView();
		GpuTextureView noHand = SceneDepth.noHandView();
		if (opaque == null) {
			opaque = main.getDepthTextureView();
		}
		if (noHand == null) {
			noHand = opaque;
		}

		GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
        assert target.getDepthTextureView() != null;
        RenderPassDescriptor descriptor = RenderPassDescriptor.builder(() -> "litematicapass_depth_seed")
			.withDepthAttachment(target.getDepthTextureView(), OptionalDouble.empty())
			.withRenderArea(new RenderPass.RenderArea(0, 0, target.width, target.height))
			.build();
		try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(descriptor)) {
			pass.setPipeline(RenderSystem.getCompiledPipeline(pipeline()));
			RenderSystem.bindDefaultUniforms(pass);
			pass.setUniform("OpaqueDepth", opaque, sampler);
			pass.setUniform("NoHandDepth", noHand, sampler);
			pass.draw(3, 1, 0, 0);
		}
	}
}
