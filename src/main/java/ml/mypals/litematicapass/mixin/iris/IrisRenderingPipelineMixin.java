package ml.mypals.litematicapass.mixin.iris;

import ml.mypals.litematicapass.render.SceneDepth;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Iris 会在主渲染通道内依次绘制第一人称手部和半透明表面，并在两次调用前后分别闭合渲染通道，
 * 因此在这两个时间点分别捕获手部与半透明绘制前的深度。
 */
@Mixin(value = IrisRenderingPipeline.class, remap = false)
public class IrisRenderingPipelineMixin {
	@Inject(method = "beginHand", at = @At("HEAD"), require = 0)
	private void litematicapass$captureBeforeHand(CallbackInfo ci) {
		SceneDepth.captureBeforeHand();
	}

	@Inject(method = "beginTranslucents", at = @At("HEAD"), require = 0)
	private void litematicapass$captureBeforeTranslucents(CallbackInfo ci) {
		SceneDepth.captureBeforeTranslucents();
	}
}
