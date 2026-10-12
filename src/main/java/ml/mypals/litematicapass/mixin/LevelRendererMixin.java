package ml.mypals.litematicapass.mixin;

import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import ml.mypals.litematicapass.render.SchematicTarget;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在LevelRender的最后的最后，我们的目标会被合并到主目标上。
 */
@Mixin(value = LevelRenderer.class, priority = 2000)
public class LevelRendererMixin {
	@Inject(method = "render", at = @At("RETURN"))
	private void litematicapass$compositeSchematic(GraphicsResourceAllocator resourceAllocator, boolean renderOutline, CameraRenderState cameraState,
		GpuBufferSlice terrainFog, Vector4f fogColor, boolean shouldRenderSky, boolean consistentDepthRequired, CallbackInfo ci) {
		SchematicTarget.compositeAndReset();
	}
}
