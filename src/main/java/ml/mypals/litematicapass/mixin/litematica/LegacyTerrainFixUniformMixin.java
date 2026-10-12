package ml.mypals.litematicapass.mixin.litematica;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ml.mypals.litematicapass.render.SchematicTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * IRIS不再接管相关渲染管线，IrisCompat.isShaderActive()在此，Litemtaica自几无需操心更多。
 * 目标按名字写：Litematica 0.26 及更早（Minecraft 1.21.11 及更早）没有这个类，此时 LitematicaPassMixinPlugin 跳过本 Mixin。
 */
@Mixin(targets = "fi.dy.masa.litematica.render.uniform.LegacyTerrainFixUniform", remap = false)
public class LegacyTerrainFixUniformMixin {
	// require = 0: Litematica 0.27 and 0.28 (Minecraft 26.1 to 26.2) keep this class as an empty stub, so there is
	// no shader-dependent uniform to correct there.
	@WrapOperation(
		method = "updateBuffer",
		at = @At(value = "INVOKE", target = "Lfi/dy/masa/litematica/compat/iris/IrisCompat;isShaderActive()Z"),
		require = 0
	)
	private boolean litematicapass$hasShadersOn(Operation<Boolean> original) {
		return original.call() && !SchematicTarget.isActive();
	}
}
