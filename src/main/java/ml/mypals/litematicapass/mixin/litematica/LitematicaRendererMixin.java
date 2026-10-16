package ml.mypals.litematicapass.mixin.litematica;

import litematica.render.LitematicaRenderer;
import ml.mypals.litematicapass.render.SchematicTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 将 schematic 地形、实体和方块实体，以及每个区块的网格叠加层（在 piecewiseRenderTranslucent 里）重定向到我们的目标中。
 * Litematica 0.40（Ornithe 1.12.2）在原版的每个方块层之后、原版实体之前调用这些方法；1.12 的实体立即绘制，没有 buffer source。
 */
@Mixin(value = LitematicaRenderer.class, remap = false)
public class LitematicaRendererMixin {
	@Inject(method = { "piecewiseRenderSolid", "piecewiseRenderCutoutMipped", "piecewiseRenderCutout", "piecewiseRenderTranslucent",
		"piecewiseRenderEntities" }, at = @At("HEAD"))
	private void litematicapass$begin(CallbackInfo ci) {
		SchematicTarget.begin();
	}

	@Inject(method = { "piecewiseRenderSolid", "piecewiseRenderCutoutMipped", "piecewiseRenderCutout", "piecewiseRenderTranslucent",
		"piecewiseRenderEntities" }, at = @At("RETURN"))
	private void litematicapass$end(CallbackInfo ci) {
		SchematicTarget.end();
	}
}
