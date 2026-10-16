package ml.mypals.litematicapass.mixin.litematica;

import litematica.render.schematic.VboRenderListSchematic;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Fixes Litematica’s schematic VBO rendering under OptiFine shaders by using the current block
 * vertex format’s stride instead of the hardcoded 28-byte vanilla stride.
 */
@Mixin(value = VboRenderListSchematic.class, remap = false)
public class VboRenderListSchematicMixin {
	@ModifyConstant(method = "setupArrayPointersBlocks", constant = @Constant(intValue = 28))
	private int litematicapass$blockStride(int stride) {
		return DefaultVertexFormats.BLOCK.getSize();
	}
}
