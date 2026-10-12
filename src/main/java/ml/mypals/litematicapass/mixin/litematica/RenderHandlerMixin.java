package ml.mypals.litematicapass.mixin.litematica;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import fi.dy.masa.litematica.event.RenderHandler;
import ml.mypals.litematicapass.render.SchematicTarget;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 重定向 Litematica 的选区， 校验盒等的渲染。
 */
@Mixin(value = RenderHandler.class, remap = false)
public class RenderHandlerMixin {
	@WrapMethod(method = "onRenderWorldLast")
	private void litematicapass$routeBoxes(RenderTarget fb, CameraRenderState cameraState, Frustum culling, RenderBuffers buffers,
		GpuBufferSlice terrainFog, Vector4f fogColor, ProfilerFiller profiler, Operation<Void> original) {
		SchematicTarget.begin();
		try {
			original.call(fb, cameraState, culling, buffers, terrainFog, fogColor, profiler);
		} finally {
			SchematicTarget.end();
		}
	}
}
