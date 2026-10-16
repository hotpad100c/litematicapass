package ml.mypals.litematicapass.mixin.litematica;

import litematica.render.schematic.ChunkRenderDispatcherLitematica;
import litematica.render.schematic.RenderGlobalSchematic;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * OptiFine 开关光影包时会替换 DefaultVertexFormats.BLOCK（有光影时每个顶点 56 字节，没有时 28 字节），然后重新加载渲染器。
 * Litematica 的区块构建缓冲（VertexBuilderCache）只在第一次创建 dispatcher 时按当时的格式建立，之后一直沿用：
 * 格式变了以后，烘焙模型的顶点数据与缓冲的格式不一致，构建区块时 BufferBuilder 越界，游戏崩溃（"Tesselating block model"）。
 * 这里在格式变化后的重新加载时丢掉旧的 dispatcher，让 Litematica 按新格式重建。这与重定向无关，但没有它就不能在光影下使用 Litematica。
 */
@Mixin(value = RenderGlobalSchematic.class, remap = false)
public class RenderGlobalSchematicMixin {
	@Shadow
	private ChunkRenderDispatcherLitematica renderDispatcher;

	@Unique
	private VertexFormat litematicapass$dispatcherFormat;

	// loadRenderers overrides RenderGlobal's, so its name is Minecraft's and is remapped; the rest of the class is Litematica's.
	@Inject(method = "loadRenderers", at = @At("HEAD"), remap = true)
	private void litematicapass$rebuildForFormat(CallbackInfo ci) {
		VertexFormat current = DefaultVertexFormats.BLOCK;
		if (this.renderDispatcher != null && this.litematicapass$dispatcherFormat != null
			&& this.litematicapass$dispatcherFormat.getSize() != current.getSize()) {
			this.renderDispatcher.stopWorkerThreads();
			this.renderDispatcher = null;
		}
		this.litematicapass$dispatcherFormat = current;
	}
}
