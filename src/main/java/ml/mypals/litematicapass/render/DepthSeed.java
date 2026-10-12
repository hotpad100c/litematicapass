package ml.mypals.litematicapass.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.renderer.RenderPipelines;

/**
 * 初始化 schematic 目标的深度，该深度决定了什么会遮挡 schematic。
 * - 它从Solid阶段之后的场景深度开始，由 {@link SceneDepth} 在绘制半透明表面之前捕获，不包含半透明深度。
 * - 在绘制第一人称手部的位置，它被初始化为最近距离，以确保手部始终显示在最上方。
 */
final class DepthSeed {

	private static RenderPipeline pipeline;

	private static RenderPipeline pipeline() {
		if (pipeline == null) {
			pipeline = RenderPipelines.register(Gpu.depthOnly(Gpu.screenQuad("pipeline/depth_seed", "core/depth_seed", "OpaqueDepth", "NoHandDepth"))
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

        assert target.getDepthTextureView() != null;
		try (RenderPass pass = Gpu.openPass("litematicapass_depth_seed", Gpu.depthPassColor(target), target.getDepthTextureView(), target.width, target.height)) {
			Gpu.setPipeline(pass, pipeline());
			RenderSystem.bindDefaultUniforms(pass);
			Gpu.bindTexture(pass, "OpaqueDepth", opaque);
			Gpu.bindTexture(pass, "NoHandDepth", noHand);
			Gpu.drawScreenQuad(pass);
		}
	}
}
