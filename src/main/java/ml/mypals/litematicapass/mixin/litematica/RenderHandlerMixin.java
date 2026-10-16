package ml.mypals.litematicapass.mixin.litematica;

import litematica.event.RenderHandler;
import ml.mypals.litematicapass.render.SchematicTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
