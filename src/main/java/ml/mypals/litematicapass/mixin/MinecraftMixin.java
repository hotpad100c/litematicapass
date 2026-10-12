package ml.mypals.litematicapass.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.pipeline.RenderTarget;
import ml.mypals.litematicapass.render.SchematicTarget;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 1.21.6 之前没有 RenderSystem 的输出目标覆盖：Litematica 的地形、实体等都画进 getMainRenderTarget()。
 * 重定向期间把它换成我们的目标。只在这些版本上应用（见 LitematicaPassMixinPlugin）。
 */
@Mixin(Minecraft.class)
public class MinecraftMixin {
	@ModifyReturnValue(method = "getMainRenderTarget", at = @At("RETURN"))
	private RenderTarget litematicapass$routeMainTarget(RenderTarget original) {
		return SchematicTarget.targetFor(original);
	}
}
