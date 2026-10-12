package ml.mypals.litematicapass.mixin.litematica;

import org.spongepowered.asm.mixin.Mixin;
//? if <26.3 && >=1.21.6 {
/*import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import ml.mypals.litematicapass.render.SchematicTarget;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import org.spongepowered.asm.mixin.injection.At;
*///?}

/**
 * Litematica 0.28 及更早(Minecraft 26.2 及更早)的地形批量绘制自己通过 {@code ChunkSectionLayerGroup.outputTarget()}
 * 选择输出目标;重定向期间改为我们的目标。26.3 起目标由参数传入(见 LitematicaRendererMixin),此 Mixin 不应用
 * (见 LitematicaPassMixinPlugin)。
 */
@Mixin(targets = "fi.dy.masa.litematica.render.schematic.ChunkRenderBatchDraw", remap = false)
public class ChunkRenderBatchDrawMixin {
	//? if <26.3 && >=1.21.6 {
	/*@WrapOperation(method = "draw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/ChunkSectionLayerGroup;outputTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;", remap = true))
	private RenderTarget litematicapass$routeOutput(ChunkSectionLayerGroup group, Operation<RenderTarget> original) {
		return SchematicTarget.targetFor(original.call(group));
	}
	*///?}
}
