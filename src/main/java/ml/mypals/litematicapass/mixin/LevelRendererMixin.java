package ml.mypals.litematicapass.mixin;

import ml.mypals.litematicapass.render.SchematicTarget;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在 LevelRenderer.render 的最后，把我们的目标合并到主目标上。
 * 不捕获参数，所以不依赖各个 Minecraft 版本中 render 的参数列表。
 */
@Mixin(value = LevelRenderer.class, priority = 2000)
public class LevelRendererMixin {
	// 26.2 renamed renderLevel to render.
	@Inject(method = { "render", "renderLevel" }, at = @At("RETURN"))
	private void litematicapass$compositeSchematic(CallbackInfo ci) {
		SchematicTarget.compositeAndReset();
	}
}
