package ml.mypals.litematicapass.mixin.litematica;

import litematica.render.LitematicaRenderer;
import ml.mypals.litematicapass.render.DeferredSchematic;
import ml.mypals.litematicapass.render.SchematicTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LitematicaRenderer.class, remap = false)
public class LitematicaRendererMixin {
	@Inject(method = "piecewiseRenderSolid", at = @At("HEAD"), cancellable = true)
	private void litematicapass$beginSolid(boolean renderColliding, float partialTicks, CallbackInfo ci) {
		LitematicaRenderer self = (LitematicaRenderer) (Object) this;
		if (DeferredSchematic.defer(() -> self.piecewiseRenderSolid(renderColliding, partialTicks))) {
			ci.cancel();
			return;
		}
		if (DeferredSchematic.isReplaying()) {
			DeferredSchematic.prepareLayer(false, false);
		}
		SchematicTarget.begin();
	}

	@Inject(method = "piecewiseRenderCutoutMipped", at = @At("HEAD"), cancellable = true)
	private void litematicapass$beginCutoutMipped(boolean renderColliding, float partialTicks, CallbackInfo ci) {
		LitematicaRenderer self = (LitematicaRenderer) (Object) this;
		if (DeferredSchematic.defer(() -> self.piecewiseRenderCutoutMipped(renderColliding, partialTicks))) {
			ci.cancel();
			return;
		}
		if (DeferredSchematic.isReplaying()) {
			DeferredSchematic.prepareLayer(true, false);
		}
		SchematicTarget.begin();
	}

	@Inject(method = "piecewiseRenderCutout", at = @At("HEAD"), cancellable = true)
	private void litematicapass$beginCutout(boolean renderColliding, float partialTicks, CallbackInfo ci) {
		LitematicaRenderer self = (LitematicaRenderer) (Object) this;
		if (DeferredSchematic.defer(() -> self.piecewiseRenderCutout(renderColliding, partialTicks))) {
			ci.cancel();
			return;
		}
		if (DeferredSchematic.isReplaying()) {
			DeferredSchematic.prepareLayer(true, true);
		}
		SchematicTarget.begin();
	}

	@Inject(method = "piecewiseRenderCutout", at = @At("RETURN"))
	private void litematicapass$endCutout(boolean renderColliding, float partialTicks, CallbackInfo ci) {
		if (DeferredSchematic.isReplaying()) {
			DeferredSchematic.finishLayer(true);
		}
	}

	@Inject(method = "piecewiseRenderEntities", at = @At("HEAD"), cancellable = true)
	private void litematicapass$beginEntities(float partialTicks, CallbackInfo ci) {
		LitematicaRenderer self = (LitematicaRenderer) (Object) this;
		if (DeferredSchematic.defer(() -> self.piecewiseRenderEntities(partialTicks))) {
			ci.cancel();
			return;
		}
		if (DeferredSchematic.isReplaying()) {
			DeferredSchematic.prepareLayer(true, false);
		}
		SchematicTarget.begin();
	}
	@Inject(method = "piecewiseRenderTranslucent", at = @At("HEAD"))
	private void litematicapass$beginTranslucent(CallbackInfo ci) {
		DeferredSchematic.replay();
		SchematicTarget.begin();
	}

	@Inject(method = { "piecewiseRenderSolid", "piecewiseRenderCutoutMipped", "piecewiseRenderCutout", "piecewiseRenderTranslucent",
		"piecewiseRenderEntities" }, at = @At("RETURN"))
	private void litematicapass$end(CallbackInfo ci) {
		SchematicTarget.end();
	}
}
