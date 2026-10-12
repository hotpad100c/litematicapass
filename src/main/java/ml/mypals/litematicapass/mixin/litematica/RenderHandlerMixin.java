package ml.mypals.litematicapass.mixin.litematica;

import fi.dy.masa.litematica.event.RenderHandler;
import ml.mypals.litematicapass.render.SchematicTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 重定向 Litematica 的选区、校验器等的渲染。
 * 不捕获参数，所以不依赖各个 Litematica 版本中 onRenderWorldLast 的参数列表（0.26 及更早叫 onRenderWorldLastAdvanced）。
 */
@Mixin(value = RenderHandler.class, remap = false)
public class RenderHandlerMixin {
	@Inject(method = { "onRenderWorldLast", "onRenderWorldLastAdvanced" }, at = @At("HEAD"))
	private void litematicapass$beginBoxes(CallbackInfo ci) {
		SchematicTarget.begin();
	}

	@Inject(method = { "onRenderWorldLast", "onRenderWorldLastAdvanced" }, at = @At("RETURN"))
	private void litematicapass$endBoxes(CallbackInfo ci) {
		SchematicTarget.end();
	}
}
