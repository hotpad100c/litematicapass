package ml.mypals.litematicapass.mixin.litematica;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fi.dy.masa.litematica.render.schematic.ChunkRenderDispatcherBuffers;
import fi.dy.masa.litematica.render.schematic.ChunkRenderTaskSchematic;
import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo;
import ml.mypals.litematicapass.compat.IrisCompat;
import org.spongepowered.asm.mixin.Mixin;

/**
 *IRIS 会扩展每个通过代码创建的 BufferBuilder 的 BLOCK 顶点格式，
 * 我们在此使用 IrisCompat.setSkipExtension 跳过扩展。
 */
@Mixin(value = ChunkRendererSchematicVbo.class, remap = false)
public class ChunkRendererSchematicVboMixin {
	@WrapMethod(method = "rebuildChunk")
	private void litematicapass$plainFormatRebuild(ChunkRenderTaskSchematic task, ChunkRenderDispatcherBuffers pack, Operation<Void> original) {
		boolean previous = IrisCompat.skipExtension();
		IrisCompat.setSkipExtension(true);
		try {
			original.call(task, pack);
		} finally {
			IrisCompat.setSkipExtension(previous);
		}
	}

	@WrapMethod(method = "resortTransparency")
	private void litematicapass$plainFormatResort(ChunkRenderTaskSchematic task, ChunkRenderDispatcherBuffers pack, Operation<Void> original) {
		boolean previous = IrisCompat.skipExtension();
		IrisCompat.setSkipExtension(true);
		try {
			original.call(task, pack);
		} finally {
			IrisCompat.setSkipExtension(previous);
		}
	}
}
