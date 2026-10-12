package ml.mypals.litematicapass.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 开发环境里 malilib 会把 SharedConstants.IS_RUNNING_IN_IDE 置为 true，使渲染后端开启严格校验，
 * Iris 的 CenterDepthSampler 因缺 InSampler 而崩溃。这里让 STRICT_VALIDATION 恒为 false。
 */
@Mixin(targets = "com.mojang.renderpearl.frontend.FrontendGpuDevice", remap = false)
public class StrictValidationMixin {
	@ModifyExpressionValue(method = "<clinit>", at = @At(value = "FIELD", target = "Lnet/minecraft/SharedConstants;IS_RUNNING_IN_IDE:Z"), require = 0)
	private static boolean selectiveRendering$disableStrictValidation(boolean original) {
		return false;
	}
}
