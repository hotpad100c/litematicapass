package ml.mypals.litematicapass.mixin.litematica;

import org.spongepowered.asm.mixin.Unique;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fi.dy.masa.litematica.render.schematic.ChunkRenderTaskSchematic;
import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo;
import ml.mypals.litematicapass.compat.IrisCompat;
import org.spongepowered.asm.mixin.Mixin;
//? if >=1.21.11 {
import fi.dy.masa.litematica.render.schematic.ChunkRenderDispatcherBuffers;
//?} else {
/*import net.minecraft.util.profiling.ProfilerFiller;
*///?}

/**
 * Iris 在渲染世界时会把原版地形顶点格式扩展成自己的格式。schematic 的网格在单独线程上构建，
 * 且构建时机与绘制时机不同，因此必须按 Iris 实际绘制时绑定给这些管线的格式来生成：
 * 26.3 及以后用 IrisCompat.setSkipExtension 让两边都留在原版格式（见 SchematicTarget），
 * 26.2 上 Iris 无法跳过扩展，只能让网格也扩展（见 IrisCompat.schematicMeshesWidened）。
 */
@Mixin(value = ChunkRendererSchematicVbo.class, remap = false)
public class ChunkRendererSchematicVboMixin {
	//? if >=1.21.11 {
	@WrapMethod(method = "rebuildChunk")
	private void litematicapass$matchingFormatRebuild(ChunkRenderTaskSchematic task, ChunkRenderDispatcherBuffers pack, Operation<Void> original) {
		matchingFormat(() -> original.call(task, pack));
	}

	@WrapMethod(method = "resortTransparency")
	private void litematicapass$matchingFormatResort(ChunkRenderTaskSchematic task, ChunkRenderDispatcherBuffers pack, Operation<Void> original) {
		matchingFormat(() -> original.call(task, pack));
	}
	//?} else {
	/*// Litematica 0.24 and older (Minecraft 1.21.10 and older) take the profiler instead of the buffers.
	@WrapMethod(method = "rebuildChunk")
	private void litematicapass$matchingFormatRebuild(ChunkRenderTaskSchematic task, ProfilerFiller profiler, Operation<Void> original) {
		matchingFormat(() -> original.call(task, profiler));
	}

	@WrapMethod(method = "resortTransparency")
	private void litematicapass$matchingFormatResort(ChunkRenderTaskSchematic task, ProfilerFiller profiler, Operation<Void> original) {
		matchingFormat(() -> original.call(task, profiler));
	}
	*///?}

	@Unique
    private static void matchingFormat(Runnable build) {
		if (IrisCompat.schematicMeshesWidened()) {
			boolean previous = IrisCompat.isRenderingLevel();
			IrisCompat.setRenderingLevel(true);
			try {
				build.run();
			} finally {
				IrisCompat.setRenderingLevel(previous);
			}
			return;
		}

		boolean previous = IrisCompat.skipExtension();
		IrisCompat.setSkipExtension(true);
		try {
			build.run();
		} finally {
			IrisCompat.setSkipExtension(previous);
		}
	}
}
