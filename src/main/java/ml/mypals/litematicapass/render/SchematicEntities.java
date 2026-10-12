package ml.mypals.litematicapass.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import ml.mypals.litematicapass.compat.IrisCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
//? if >=26.3 {
import com.mojang.renderpearl.api.commands.RenderPass;
import java.util.Optional;
import java.util.OptionalDouble;
//?} else {
/*import com.mojang.renderpearl.api.textures.GpuTextureView;
*///?}

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
		//? if >=26.2 {
		if (storage == null) {
			storage = new SubmitNodeStorage();
		}
		//?} else {
		/*// The dispatcher renders the storage it was created with.
		dispatcher();
		*///?}
		//? if >=26.3 {
		storage.setUseImprovedTransparency(Minecraft.getInstance().gameRenderer.useImprovedTransparency());
		//?}
		return storage;
	}

	private static FeatureRenderDispatcher dispatcher() {
		if (dispatcher == null) {
			Minecraft mc = Minecraft.getInstance();
			buffers = new RenderBuffers(1);
			//? if >=26.2 {
			dispatcher = new FeatureRenderDispatcher(buffers, mc.getModelManager(), mc.getAtlasManager(), mc.font, mc.gameRenderer.gameRenderState());
			//?} else if >=26.1 {
			/*storage = new SubmitNodeStorage();
			dispatcher = new FeatureRenderDispatcher(storage, mc.getModelManager(), buffers.bufferSource(), mc.getAtlasManager(),
				buffers.outlineBufferSource(), buffers.crumblingBufferSource(), mc.font, mc.gameRenderer.getGameRenderState());
			*///?} else {
			/*storage = new SubmitNodeStorage();
			dispatcher = new FeatureRenderDispatcher(storage, mc.getBlockRenderer(), buffers.bufferSource(), mc.getAtlasManager(),
				buffers.outlineBufferSource(), buffers.crumblingBufferSource(), mc.font);
			*///?}
		}
		return dispatcher;
	}

	//? if >=26.2 {
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
			//? if >=26.3 {
			if (!frame.isEmpty()) {
                assert target.getColorTextureView() != null;
                try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
					() -> "litematicapass_entities", target.getColorTextureView(), Optional.empty(), target.getDepthTextureView(), OptionalDouble.empty())) {
					RenderSystem.bindDefaultUniforms(pass);
					FeatureRenderDispatcher.renderAllFeatures(pass, frame);
				}
			}
			//?} else {
			/*// Each feature opens its own pass on the output target; point that output at our target.
			GpuTextureView savedColor = RenderSystem.outputColorTextureOverride;
			GpuTextureView savedDepth = RenderSystem.outputDepthTextureOverride;
			RenderSystem.outputColorTextureOverride = target.getColorTextureView();
			RenderSystem.outputDepthTextureOverride = target.getDepthTextureView();
			try {
				frame.executeSolid();
				frame.executeTranslucent();
				frame.executeTranslucentAfterTerrain();
				frame.executeAlwaysOnTop();
			} finally {
				RenderSystem.outputColorTextureOverride = savedColor;
				RenderSystem.outputDepthTextureOverride = savedDepth;
			}
			*///?}
		} finally {
			buffers.endFrame();
		}
	}
	//?} else {
	/*public static void draw(RenderTarget target) {
		if (dispatcher == null) {
			return;
		}
		// The features are drawn into the buffer source and reach the output target when it ends its batch; point
		// that output at our target. Iris widens the vertex formats while the vertices are written, so keep it out.
		GpuTextureView savedColor = RenderSystem.outputColorTextureOverride;
		GpuTextureView savedDepth = RenderSystem.outputDepthTextureOverride;
		boolean renderingLevel = IrisCompat.isRenderingLevel();
		RenderSystem.outputColorTextureOverride = target.getColorTextureView();
		RenderSystem.outputDepthTextureOverride = target.getDepthTextureView();
		IrisCompat.setRenderingLevel(false);
		try {
			renderFeatures();
		} finally {
			storage.clear();
			IrisCompat.setRenderingLevel(renderingLevel);
			RenderSystem.outputColorTextureOverride = savedColor;
			RenderSystem.outputDepthTextureOverride = savedDepth;
		}
	}
	*///?}

	//? if >=26.1 && <26.2 {
	/*private static void renderFeatures() {
		dispatcher.renderSolidFeatures();
		buffers.bufferSource().endBatch();
		dispatcher.renderTranslucentFeatures();
		buffers.bufferSource().endBatch();
	}
	*///?} else if <26.1 {
	/*private static void renderFeatures() {
		dispatcher.renderAllFeatures();
		buffers.bufferSource().endBatch();
	}
	*///?}

	static void endFrame() {
		//? if >=26.2 {
		storage = null;
		//?} else {
		/*if (storage != null) {
			storage.clear();
		}
		*///?}
	}

	static void close() {
		storage = null;
		if (dispatcher != null) {
			dispatcher.close();
			dispatcher = null;
		}
		if (buffers != null) {
			//? if >=26.2 {
			buffers.close();
			//?}
			buffers = null;
		}
	}
}
