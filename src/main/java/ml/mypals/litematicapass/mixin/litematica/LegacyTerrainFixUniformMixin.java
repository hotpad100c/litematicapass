package ml.mypals.litematicapass.mixin.litematica;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fi.dy.masa.litematica.compat.iris.IrisCompat;
import fi.dy.masa.litematica.render.uniform.LegacyTerrainFixUniform;
import ml.mypals.litematicapass.render.SchematicTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * IRIS不再接管相关渲染管线，IrisCompat.isShaderActive()在此，Litemtaica自几无需操心更多。
 */
@Mixin(value = LegacyTerrainFixUniform.class, remap = false)
public class LegacyTerrainFixUniformMixin {
	@WrapOperation(
		method = "updateBuffer",
		at = @At(value = "INVOKE", target = "Lfi/dy/masa/litematica/compat/iris/IrisCompat;isShaderActive()Z")
	)
	private boolean litematicapass$hasShadersOn(Operation<Boolean> original) {
		return original.call() && !SchematicTarget.isActive();
	}
}
