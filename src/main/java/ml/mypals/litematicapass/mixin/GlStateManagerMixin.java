package ml.mypals.litematicapass.mixin;

import ml.mypals.litematicapass.render.SchematicTarget;
import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fixes Litematica’s translucent overlay compositing by accumulating alpha during framebuffer
 * redirection instead of overwriting the target alpha, preventing the background from bleeding
 * through during compositing.
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
