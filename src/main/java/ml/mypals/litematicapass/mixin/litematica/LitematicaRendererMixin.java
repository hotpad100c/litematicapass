package ml.mypals.litematicapass.mixin.litematica;

import litematica.render.LitematicaRenderer;
import ml.mypals.litematicapass.render.DeferredSchematic;
import ml.mypals.litematicapass.render.SchematicTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 将 schematic 地形、实体和方块实体，以及每个区块的网格叠加层（在 piecewiseRenderTranslucent 里）重定向到我们的目标中。
 * Litematica 0.40（Ornithe 1.12.2）在原版的每个方块层之后、原版实体之前调用这些方法；1.12 的实体立即绘制，没有 buffer source。
 * 有光影包时，固体、镂空和实体推迟到 OptiFine 的 preWater 再画（见 DeferredSchematic）。
 * 每个方法只有一个 HEAD 处理：推迟时取消调用（此时不能 begin，否则取消后不会走到 RETURN 的 end）。
 */
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

	// The translucent layer comes after OptiFine's preWater; replay there whatever is still held back, as a fallback.
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
