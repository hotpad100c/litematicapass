package ml.mypals.litematicapass.mixin.litematica;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import ml.mypals.litematicapass.render.SchematicEntities;
import ml.mypals.litematicapass.render.SchematicTarget;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
//? if <26.3 && >=1.21.11 {
/*import com.mojang.renderpearl.api.textures.GpuSampler;
import org.jspecify.annotations.Nullable;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 将 schematic 地形、每个区块的网格叠加层、实体和方块实体重定向到我们的目标中。 */
@Mixin(value = LitematicaRenderer.class, remap = false)
public class LitematicaRendererMixin {
	//? if >=26.3 {
	@WrapMethod(method = "piecewiseDrawBlockLayerGroup")
	private void litematicapass$routeTerrain(RenderTarget fb, ChunkSectionLayerGroup group, Operation<Void> original) {
		SchematicTarget.begin();
		try {
			RenderTarget target = SchematicTarget.targetFor(fb);
			original.call(target, group);
			// The entities and block entities go into the same target, after the opaque terrain.
			if (SchematicTarget.isRouting() && group == ChunkSectionLayerGroup.OPAQUE) {
				SchematicEntities.draw(target);
			}
		} finally {
			SchematicTarget.end();
		}
	}
	//?} else if >=1.21.11 {
	/*// Litematica picks the output target itself here (see ChunkRenderBatchDrawMixin).
	@WrapMethod(method = "piecewiseDrawBlockLayerGroup")
	private void litematicapass$routeTerrain(ChunkSectionLayerGroup group, @Nullable GpuSampler sampler, Operation<Void> original) {
		SchematicTarget.begin();
		try {
			original.call(group, sampler);
			// The entities and block entities go into the same target, after the opaque terrain.
			if (SchematicTarget.isRouting() && group == ChunkSectionLayerGroup.OPAQUE) {
				SchematicEntities.draw(SchematicTarget.prepareForDraw());
			}
		} finally {
			SchematicTarget.end();
		}
	}
	*///?} else {
	/*// Litematica picks the output target itself here (see ChunkRenderBatchDrawMixin); 0.24 and older take no sampler.
	@WrapMethod(method = "piecewiseDrawBlockLayerGroup")
	private void litematicapass$routeTerrain(ChunkSectionLayerGroup group, Operation<Void> original) {
		SchematicTarget.begin();
		try {
			original.call(group);
			// The entities and block entities go into the same target, after the opaque terrain.
			if (SchematicTarget.isRouting() && group == ChunkSectionLayerGroup.OPAQUE) {
				SchematicEntities.draw(SchematicTarget.prepareForDraw());
			}
		} finally {
			SchematicTarget.end();
		}
	}
	*///?}

	/** 实体和方块实体提交到我们自己的 SubmitNodeStorage，而不是原版的。 */
	@ModifyVariable(method = "piecewiseRenderEntities", at = @At("HEAD"), argsOnly = true)
	private SubmitNodeCollector litematicapass$routeEntities(SubmitNodeCollector queue) {
		return SchematicEntities.route(queue);
	}

	@ModifyVariable(method = "piecewiseRenderBlockEntities", at = @At("HEAD"), argsOnly = true)
	private SubmitNodeStorage litematicapass$routeBlockEntities(SubmitNodeStorage queue) {
		return SchematicEntities.route(queue);
	}

	@Inject(method = "piecewiseRenderOverlay", at = @At("HEAD"))
	private void litematicapass$beginOverlay(CallbackInfo ci) {
		SchematicTarget.begin();
	}

	@Inject(method = "piecewiseRenderOverlay", at = @At("RETURN"))
	private void litematicapass$endOverlay(CallbackInfo ci) {
		SchematicTarget.end();
	}
}
