package ml.mypals.litematicapass.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
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
			render.accept(source);
			// The batches reach the output target when they end; point that output at our target.
			Runnable restore = Gpu.redirectOutput(target);
			try {
				source.endBatch();
			} finally {
				restore.run();
			}
		} finally {
			IrisCompat.setRenderingLevel(renderingLevel);
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
		if (bytes != null) {
			bytes.close();
			bytes = null;
		}
	}
}