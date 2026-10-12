package ml.mypals.litematicapass.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
//? if >=1.21 {
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
//?} else {
/*import com.mojang.blaze3d.vertex.BufferBuilder;
*///?}
import ml.mypals.litematicapass.compat.IrisCompat;
import net.minecraft.client.renderer.MultiBufferSource;

import java.util.function.Consumer;

// Litematica 1.21.8 及更早的版本直接把 schematic 的实体和方块实体画进一个 buffer source（在地形之后）。
// 重定向期间我们给它自己的 buffer source，画完立即把它冲到我们的目标里。
public final class SchematicEntities {
	//? if >=1.21 {
	private static ByteBufferBuilder bytes;
	//?}
	private static MultiBufferSource.BufferSource source;
	private static boolean drawing;
	private static boolean renderingLevelSaved;

	/**
	 * The buffer source to draw the schematic's entities into: ours between {@link #begin} and {@link #end}, else
	 * {@code original}. For Litematica builds that take it from Minecraft's buffers themselves (before 1.21).
	 */
	public static MultiBufferSource.BufferSource routed(MultiBufferSource.BufferSource original) {
		return drawing ? source : original;
	}

	// Lets Litematica draw into our buffer source, then flushes it into the schematic target.
	public static void drawInto(Consumer<MultiBufferSource.BufferSource> render) {
		begin();
		try {
			render.accept(source);
		} finally {
			end();
		}
	}

	/** Starts drawing the schematic's entities into our buffer source (see {@link #routed}); does nothing unrouted. */
	public static void begin() {
		if (drawing || !SchematicTarget.isActive()) {
			return;
		}
		if (source == null) {
			//? if >=1.21 {
			bytes = new ByteBufferBuilder(786432);
			source = MultiBufferSource.immediate(bytes);
			//?} else {
			/*source = MultiBufferSource.immediate(new BufferBuilder(786432));
			*///?}
		}
		renderingLevelSaved = IrisCompat.isRenderingLevel();
		SchematicTarget.begin();
		SchematicTarget.prepareForDraw();
		// Iris widens the vertex formats while the vertices are written, so keep it out.
		IrisCompat.setRenderingLevel(false);
		drawing = true;
	}

	/** Flushes what was drawn since {@link #begin} into the schematic target. */
	public static void end() {
		if (!drawing) {
			return;
		}
		drawing = false;
		try {
			// The batches reach the output target when they end; point that output at our target.
			Runnable restore = Gpu.redirectOutput(SchematicTarget.prepareForDraw());
			try {
				source.endBatch();
			} finally {
				restore.run();
			}
		} finally {
			IrisCompat.setRenderingLevel(renderingLevelSaved);
			SchematicTarget.end();
		}
	}

	// The entities were already flushed in drawInto.
	public static void draw(RenderTarget target) {
	}

	static void endFrame() {
	}

	static void close() {
		source = null;
		//? if >=1.21 {
		if (bytes != null) {
			bytes.close();
			bytes = null;
		}
		//?}
	}
}