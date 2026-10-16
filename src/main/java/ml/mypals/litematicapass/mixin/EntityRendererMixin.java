package ml.mypals.litematicapass.mixin;

import ml.mypals.litematicapass.render.SchematicTarget;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Provides a per-frame fallback by compositing and resetting our framebuffer for Litematica after OptiFine’s
 * final composite pass, or at the end of the world pass for incompatible OptiFine versions.
 */
@Mixin(value = EntityRenderer.class, priority = 2000)
public class EntityRendererMixin {
	@Inject(method = "renderWorldPass(IFJ)V", at = @At("RETURN"))
	private void litematicapass$compositeSchematic(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
		SchematicTarget.compositeAndReset();
	}
}
