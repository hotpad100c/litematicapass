package ml.mypals.litematicapass.mixin.litematica;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import fi.dy.masa.litematica.render.schematic.WorldRendererSchematic;
import ml.mypals.litematicapass.render.SchematicEntities;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 1.21 之前：Litematica 把 schematic 的实体和方块实体画进 Minecraft 自己的 buffer source，由原版之后冲到主目标。
 * 重定向期间换成我们的 buffer source（见 SchematicEntities），只在这些版本上应用（见 LitematicaPassMixinPlugin）。
 */
@Mixin(value = WorldRendererSchematic.class, remap = false)
public class WorldRendererSchematicMixin {
	@ModifyExpressionValue(method = "renderEntities", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/RenderBuffers;bufferSource()Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;",
		remap = true))
	private MultiBufferSource.BufferSource litematicapass$routeBufferSource(MultiBufferSource.BufferSource original) {
		return SchematicEntities.routed(original);
	}
}
