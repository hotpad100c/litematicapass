package ml.mypals.litematicapass.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.commands.RenderPass;
import ml.mypals.litematicapass.compat.IrisCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;

import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Litematica 会将 schematic 的实体和方块实体提交到原版的　submit storage　中，
 * 而在 schematic 重定向期间，它们会被提交到我们自己的存储中，并由我们绘制到自己的目标。
 */
public final class SchematicEntities {
	private static SubmitNodeStorage storage;
	private static RenderBuffers buffers;
	private static FeatureRenderDispatcher dispatcher;
	public static SubmitNodeCollector route(SubmitNodeCollector original) {
		return SchematicTarget.isActive() ? storage() : original;
	}

	public static SubmitNodeStorage route(SubmitNodeStorage original) {
		return SchematicTarget.isActive() ? storage() : original;
	}

	private static SubmitNodeStorage storage() {
		if (storage == null) {
			storage = new SubmitNodeStorage();
		}
		storage.setUseImprovedTransparency(Minecraft.getInstance().gameRenderer.useImprovedTransparency());
		return storage;
	}

	private static FeatureRenderDispatcher dispatcher() {
		if (dispatcher == null) {
			Minecraft mc = Minecraft.getInstance();
			buffers = new RenderBuffers(1);
			dispatcher = new FeatureRenderDispatcher(buffers, mc.getModelManager(), mc.getAtlasManager(), mc.font, mc.gameRenderer.gameRenderState());
		}
		return dispatcher;
	}

	public static void draw(RenderTarget target) {
		SubmitNodeStorage submitted = storage;
		storage = null;
		if (submitted == null) {
			return;
		}

		boolean renderingLevel = IrisCompat.isRenderingLevel();
		IrisCompat.setRenderingLevel(false);
		FeatureRenderDispatcher.PreparedFrame frame;
		try {
			frame = dispatcher().prepareFrame(submitted);
		} finally {
			IrisCompat.setRenderingLevel(renderingLevel);
		}

		try (frame) {
			if (!frame.isEmpty()) {
                assert target.getColorTextureView() != null;
                try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
					() -> "litematicapass_entities", target.getColorTextureView(), Optional.empty(), target.getDepthTextureView(), OptionalDouble.empty())) {
					RenderSystem.bindDefaultUniforms(pass);
					FeatureRenderDispatcher.renderAllFeatures(pass, frame);
				}
			}
		} finally {
			buffers.endFrame();
		}
	}

	static void endFrame() {
		storage = null;
	}

	static void close() {
		storage = null;
		if (dispatcher != null) {
			dispatcher.close();
			dispatcher = null;
		}
		if (buffers != null) {
			buffers.close();
			buffers = null;
		}
	}
}
