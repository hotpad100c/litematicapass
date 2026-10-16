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
 * Fixes Litematica crashes when toggling OptiFine shaders by recreating its chunk dispatcher
 * when the block vertex format changes, keeping its vertex buffers in sync with the new format.
 */
@Mixin(value = RenderGlobalSchematic.class, remap = false)
public class RenderGlobalSchematicMixin {
	@Shadow
	private ChunkRenderDispatcherLitematica renderDispatcher;

	@Unique
	private VertexFormat litematicapass$dispatcherFormat;

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
