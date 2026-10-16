package ml.mypals.litematicapass.mixin.optifine;

import ml.mypals.litematicapass.render.SceneDepth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * OptiFine 在绘制半透明表面之前用 renderHand0 绘制（不透明的）第一人称手部，此时它的帧缓冲仍处于绑定状态；
 * 在这里捕获手部之前的深度（相当于 depthtex2）。
 */
@Pseudo
@Mixin(targets = "net.optifine.shaders.ShadersRender", remap = false)
public class ShadersRenderMixin {
	@Inject(method = "renderHand0", at = @At("HEAD"), require = 0)
	private static void litematicapass$captureBeforeHand(CallbackInfo ci) {
		SceneDepth.captureBeforeHand();
	}
}
