package ml.mypals.litematicapass.mixin.litematica;

import litematica.render.schematic.VboRenderListSchematic;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Litematica 用 VBO 绘制 schematic 地形时把顶点步长写死为 28 字节（原版方块格式）。有 OptiFine 光影时方块格式是 56 字节
 * （前 28 字节的布局不变，后面是法线等光影数据），区块按那个格式构建，按 28 字节读就是乱的。步长改为当前方块格式的大小
 * （RenderGlobalSchematicMixin 保证区块按当前格式构建）。
 */
@Mixin(value = VboRenderListSchematic.class, remap = false)
public class VboRenderListSchematicMixin {
	@ModifyConstant(method = "setupArrayPointersBlocks", constant = @Constant(intValue = 28))
	private int litematicapass$blockStride(int stride) {
		return DefaultVertexFormats.BLOCK.getSize();
	}
}
