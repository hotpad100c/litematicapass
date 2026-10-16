package ml.mypals.litematicapass.mixin.optifine;

import ml.mypals.litematicapass.render.SceneDepth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Pseudo
@Mixin(targets = "net.optifine.shaders.ShadersRender", remap = false)
public class ShadersRenderMixin {
	@Inject(method = "renderHand0", at = @At("HEAD"), require = 0)
	private static void litematicapass$captureBeforeHand(CallbackInfo ci) {
		SceneDepth.captureBeforeHand();
	}
}
