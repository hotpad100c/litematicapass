package ml.mypals.litematicapass.mixin;

import ml.mypals.litematicapass.render.SchematicTarget;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 每帧状态的兜底收尾：正常情况下 ShadersMixin 在 OptiFine 的 renderCompositeFinal 之后合并并重置；
 * 若那个挂接点不存在（其它 OptiFine 版本），就在 renderWorldPass 的最后合并（此时手部覆盖层已经画上，可能被盖住）。
 */
@Mixin(value = EntityRenderer.class, priority = 2000)
public class EntityRendererMixin {
	@Inject(method = "renderWorldPass(IFJ)V", at = @At("RETURN"))
	private void litematicapass$compositeSchematic(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
		SchematicTarget.compositeAndReset();
	}
}
