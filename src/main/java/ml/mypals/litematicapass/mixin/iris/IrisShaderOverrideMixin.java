package ml.mypals.litematicapass.mixin.iris;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import ml.mypals.litematicapass.compat.IrisCompat;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 26.3 之前：重定向期间让 Iris 不想替换着色器。Iris 1.8.1 及更早没有 ImmediateState.bypass，这就是 bypass；
 * 有 bypass 的版本里，只要 Iris 想替换，原版着色器就会被它锁住颜色和深度写入（什么都画不出）。只在这些版本上应用（见 LitematicaPassMixinPlugin）。
 */
@Mixin(value = IrisRenderingPipeline.class, remap = false)
public class IrisShaderOverrideMixin {
	@ModifyReturnValue(method = "shouldOverrideShaders", at = @At("RETURN"))
	private boolean litematicapass$bypass(boolean original) {
		return original && !IrisCompat.bypass();
	}
}
