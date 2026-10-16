package ml.mypals.litematicapass.mixin.litematica;

import litematica.event.RenderHandler;
import ml.mypals.litematicapass.render.SchematicTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 重定向 Litematica 的选区、校验器等的渲染，以及关闭 "Better Render Order" 时整个 schematic 的渲染（renderSchematicWorld）。
 * malilib 在 renderWorldPass 读取 renderHand 字段处触发这个事件：在 OptiFine 下是半透明之后、renderCompositeFinal 之前。
 */
@Mixin(value = RenderHandler.class, remap = false)
public class RenderHandlerMixin {
	@Inject(method = "onPostWorldRender", at = @At("HEAD"))
	private void litematicapass$beginBoxes(CallbackInfo ci) {
		SchematicTarget.begin();
	}

	@Inject(method = "onPostWorldRender", at = @At("RETURN"))
	private void litematicapass$endBoxes(CallbackInfo ci) {
		SchematicTarget.end();
	}
}
