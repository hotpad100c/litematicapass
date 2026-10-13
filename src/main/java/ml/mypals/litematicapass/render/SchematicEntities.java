package ml.mypals.litematicapass.render;

//? if >=1.21.9 {
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
	private static boolean submitted;

	public static SubmitNodeCollector route(SubmitNodeCollector original) {
		return SchematicTarget.isActive() ? submitTo() : original;
	}

	public static SubmitNodeStorage route(SubmitNodeStorage original) {
		return SchematicTarget.isActive() ? submitTo() : original;
	}

	private static SubmitNodeStorage submitTo() {
		submitted = true;
		return storage();
	}

	/**
	 * Whether there are entities to draw at the end of the frame: before 26.3 Litematica submits them during the
	 * level's entity phase, after its terrain was drawn (26.3 draws them with the opaque terrain).
	 */
	static boolean pendingAtEndOfFrame() {
		//? if >=26.3 {
		return false;
		//?} else {
		/*return submitted;
		*///?}
	}

	/** Draws what was submitted since the last time (see {@link #pendingAtEndOfFrame}). */
	static void drawPending(RenderTarget target) {
		submitted = false;
		draw(target);
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

		// The vertices are written here. 26.3's Iris widens the vertex formats while the level renders, but keeps the
		// vanilla pipelines we draw with on the vanilla formats: keep it out. 26.2's Iris hands out its widened entity
		// formats as the vanilla ones (as for the terrain), so the vertices must be widened to match (else they come
		// out as stretched triangles), whenever in the frame this runs.
		boolean renderingLevel = IrisCompat.isRenderingLevel();
		//? if >=26.3 {
		IrisCompat.setRenderingLevel(false);
		//?} else {
		/*IrisCompat.setRenderingLevel(true);
		*///?}
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
		submitted = false;
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
//?} else {
/*import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import ml.mypals.litematicapass.compat.IrisCompat;
import net.minecraft.client.renderer.MultiBufferSource;

import java.util.function.Consumer;

// Litematica 1.21.8 及更早的版本直接把 schematic 的实体和方块实体画进一个 buffer source（在地形之后）。
// 重定向期间我们给它自己的 buffer source，画完立即把它冲到我们的目标里。
public final class SchematicEntities {
	private static ByteBufferBuilder bytes;
	private static MultiBufferSource.BufferSource source;

	// Lets Litematica draw into our buffer source, then flushes it into the schematic target.
	public static void drawInto(Consumer<MultiBufferSource.BufferSource> render) {
		if (source == null) {
			bytes = new ByteBufferBuilder(786432);
			source = MultiBufferSource.immediate(bytes);
		}
		boolean renderingLevel = IrisCompat.isRenderingLevel();
		SchematicTarget.begin();
		try {
			RenderTarget target = SchematicTarget.prepareForDraw();
			// Iris widens the vertex formats while the vertices are written, so keep it out.
			IrisCompat.setRenderingLevel(false);
			// The batches reach the output target when they end, and Litematica ends some itself while it draws; point
			// that output at our target for all of it.
			Runnable restore = Gpu.redirectOutput(target);
			try {
				render.accept(source);
				source.endBatch();
			} finally {
				restore.run();
			}
		} finally {
			IrisCompat.setRenderingLevel(renderingLevel);
			SchematicTarget.end();
		}
	}

	// The entities are flushed in drawInto, nothing is left for the end of the frame.
	static boolean pendingAtEndOfFrame() {
		return false;
	}

	static void drawPending(RenderTarget target) {
	}

	// The entities were already flushed in drawInto.
	public static void draw(RenderTarget target) {
	}

	static void endFrame() {
	}

	static void close() {
		source = null;
		if (bytes != null) {
			bytes.close();
			bytes = null;
		}
	}
}
*///?}