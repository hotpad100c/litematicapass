package ml.mypals.litematicapass.mixin.litematica;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fi.dy.masa.litematica.render.schematic.ChunkRenderTaskSchematic;
import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo;
import ml.mypals.litematicapass.compat.IrisCompat;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 *IRIS 会扩展每个通过代码创建的 BufferBuilder 的 BLOCK 顶点格式，
 * 我们在此使用 IrisCompat.setSkipExtension 跳过扩展。
 */
@Mixin(value = ChunkRendererSchematicVbo.class, remap = false)
public class ChunkRendererSchematicVboMixin {
	// Litematica 0.24 and older (Minecraft 1.21.10 and older) take the profiler instead of the buffers.
	@WrapMethod(method = "rebuildChunk")
	private void litematicapass$plainFormatRebuild(ChunkRenderTaskSchematic task, ProfilerFiller profiler, Operation<Void> original) {
		plainFormat(() -> original.call(task, profiler));
	}

	@WrapMethod(method = "resortTransparency")
	private void litematicapass$plainFormatResort(ChunkRenderTaskSchematic task, ProfilerFiller profiler, Operation<Void> original) {
		plainFormat(() -> original.call(task, profiler));
	}

	private static void plainFormat(Runnable build) {
		boolean previous = IrisCompat.skipExtension();
		IrisCompat.setSkipExtension(true);
		try {
			build.run();
		} finally {
			IrisCompat.setSkipExtension(previous);
		}
	}
}
