package ml.mypals.litematicapass.mixin.litematica;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.pipeline.RenderTarget;
import fi.dy.masa.malilib.render.RenderUtils;
import ml.mypals.litematicapass.render.SchematicTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Litematica 的加层和 malilib 的 RenderContext 都会在这里查找它们的绘制目标。
 * 在 schematic 绘制被重定向期间，它们会获取我们的目标，而不是主目标。
 */
@Mixin(value = RenderUtils.class, remap = false)
public class RenderUtilsMixin {
	@ModifyReturnValue(method = "mainTarget", at = @At("RETURN"))
	private static RenderTarget litematicapass$routeMainTarget(RenderTarget original) {
		return SchematicTarget.targetFor(original);
	}
}
