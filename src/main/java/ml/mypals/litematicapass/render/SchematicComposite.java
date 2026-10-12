package ml.mypals.litematicapass.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
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
			pipeline = RenderPipelines.register(Gpu.colorOutput(Gpu.screenQuad("pipeline/schematic_composite", "core/schematic_composite",
					"SchematicColor", "SchematicDepth", "MainDepth", "OpaqueDepth"),
				Optional.of(Gpu.PREMULTIPLIED_ALPHA))
				.build());
		}
		return pipeline;
	}

	static void draw(RenderTarget main, RenderTarget schematic) {
		RenderPipeline pipeline = pipeline();
		try (RenderPass pass = Gpu.colorPass("litematicapass_schematic_composite", main)) {
			Gpu.setPipeline(pass, pipeline);
			RenderTarget opaque = SceneDepth.opaque();
			Gpu.bindColor(pass, "SchematicColor", schematic);
			Gpu.bindDepth(pass, "SchematicDepth", schematic);
			Gpu.bindDepth(pass, "MainDepth", main);
			Gpu.bindDepth(pass, "OpaqueDepth", opaque != null ? opaque : main);
			Gpu.drawScreenQuad(pass);
		}
	}
}
