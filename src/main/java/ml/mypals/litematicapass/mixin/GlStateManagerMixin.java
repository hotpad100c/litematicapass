package ml.mypals.litematicapass.mixin;

import ml.mypals.litematicapass.render.SchematicTarget;
import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Litematica 的半透明 schematic 和覆盖层用 (SRC_ALPHA, ONE_MINUS_SRC_ALPHA, ONE, ZERO) 绘制，其 alpha 因子 (ONE, ZERO)
 * 会把我们目标里已有的 alpha 换成覆盖层自己的 alpha（约 0.5），合并时背景就透了出来（画面过亮）。重定向期间把 alpha
 * 因子换成 (ONE, ONE_MINUS_SRC_ALPHA)，让 alpha 像覆盖率一样累积；颜色因子不变。
 */
@Mixin(GlStateManager.class)
public class GlStateManagerMixin {
	private static final int ONE = 1;
	private static final int ONE_MINUS_SRC_ALPHA = 0x0303;

	@ModifyVariable(method = "tryBlendFuncSeparate(IIII)V", at = @At("HEAD"), ordinal = 2, argsOnly = true)
	private static int litematicapass$sourceAlpha(int factor) {
		return SchematicTarget.isRouting() ? ONE : factor;
	}

	@ModifyVariable(method = "tryBlendFuncSeparate(IIII)V", at = @At("HEAD"), ordinal = 3, argsOnly = true)
	private static int litematicapass$destinationAlpha(int factor) {
		return SchematicTarget.isRouting() ? ONE_MINUS_SRC_ALPHA : factor;
	}

	@Inject(method = "blendFunc(II)V", at = @At("HEAD"), cancellable = true)
	private static void litematicapass$separateAlpha(int source, int destination, CallbackInfo ci) {
		if (SchematicTarget.isRouting()) {
			GlStateManager.tryBlendFuncSeparate(source, destination, ONE, ONE_MINUS_SRC_ALPHA);
			ci.cancel();
		}
	}
}
