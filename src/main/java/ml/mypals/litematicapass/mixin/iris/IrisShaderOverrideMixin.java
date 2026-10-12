package ml.mypals.litematicapass.mixin.iris;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import ml.mypals.litematicapass.compat.IrisCompat;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Iris 1.8.1 及更早（Minecraft 1.21.3 及更早）没有 ImmediateState.bypass：重定向期间让 Iris 不替换着色器，
 * 和新版 Iris 的 bypass 效果相同。只在这些版本上应用（见 LitematicaPassMixinPlugin）。
 */
@Mixin(value = IrisRenderingPipeline.class, remap = false)
public class IrisShaderOverrideMixin {
	@ModifyReturnValue(method = "shouldOverrideShaders", at = @At("RETURN"))
	private boolean litematicapass$bypass(boolean original) {
		return original && !IrisCompat.bypass();
	}
}
