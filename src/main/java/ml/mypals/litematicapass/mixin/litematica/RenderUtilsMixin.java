package ml.mypals.litematicapass.mixin.litematica;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import litematica.render.RenderUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Fixes Litematica’s missing-block overlay collapsing under OptiFine
 * shaders by repacking shader-expanded BakedQuad vertex data into the vanilla
 * 7-int-per-vertex format expected by Litematica.
 */
@Mixin(value = RenderUtils.class, remap = false)
public class RenderUtilsMixin {
	private static final int VANILLA_INTS = 7;

	@ModifyExpressionValue(method = { "renderModelQuadOverlayBatched", "renderModelQuadOutlines" }, at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/block/model/BakedQuad;getVertexData()[I", remap = true))
	private static int[] litematicapass$vanillaVertexLayout(int[] vertexData) {
		int stride = vertexData.length / 4;
		if (stride <= VANILLA_INTS) {
			return vertexData;
		}
		int[] compact = new int[4 * VANILLA_INTS];
		for (int vertex = 0; vertex < 4; vertex++) {
			System.arraycopy(vertexData, vertex * stride, compact, vertex * VANILLA_INTS, VANILLA_INTS);
		}
		return compact;
	}
}
