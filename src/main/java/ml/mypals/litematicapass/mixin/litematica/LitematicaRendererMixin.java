package ml.mypals.litematicapass.mixin.litematica;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import ml.mypals.litematicapass.render.SchematicEntities;
import ml.mypals.litematicapass.render.SchematicTarget;
//? if >=1.21.9 {
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
//?} else {
/*import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.profiling.ProfilerFiller;
*///?}
//? if >=1.21.6 {
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
//?}
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
			// The entities and block entities are submitted later in the frame and drawn at its end (see SchematicTarget).
			original.call(group, sampler);
		} finally {
			SchematicTarget.end();
		}
	}
	*///?} else if >=1.21.6 {
	/*// Litematica picks the output target itself here (see ChunkRenderBatchDrawMixin); 0.24 and older take no sampler.
	@WrapMethod(method = "piecewiseDrawBlockLayerGroup")
	private void litematicapass$routeTerrain(ChunkSectionLayerGroup group, Operation<Void> original) {
		SchematicTarget.begin();
		try {
			// From 1.21.9 the entities and block entities are submitted later in the frame and drawn at its end (see
			// SchematicTarget); before, they are drawn by themselves (see below).
			original.call(group);
		} finally {
			SchematicTarget.end();
		}
	}
	*///?} else {
	/*// Litematica 0.22 and older (Minecraft 1.21.5 and older) draw each terrain layer into the main target, which
	// MinecraftMixin hands out as the schematic target while routing.
	@Inject(method = { "piecewiseRenderSolid", "piecewiseRenderCutoutMipped", "piecewiseRenderCutout", "piecewiseRenderTranslucent",
		"piecewiseRenderTripwire" }, at = @At("HEAD"))
	private void litematicapass$beginTerrain(CallbackInfo ci) {
		SchematicTarget.begin();
	}

	@Inject(method = { "piecewiseRenderSolid", "piecewiseRenderCutoutMipped", "piecewiseRenderCutout", "piecewiseRenderTranslucent",
		"piecewiseRenderTripwire" }, at = @At("RETURN"))
	private void litematicapass$endTerrain(CallbackInfo ci) {
		SchematicTarget.end();
	}
	*///?}

	//? if >=1.21.9 {
	/** 实体和方块实体提交到我们自己的 SubmitNodeStorage，而不是原版的。 */
	@ModifyVariable(method = "piecewiseRenderEntities", at = @At("HEAD"), argsOnly = true)
	private SubmitNodeCollector litematicapass$routeEntities(SubmitNodeCollector queue) {
		return SchematicEntities.route(queue);
	}

	@ModifyVariable(method = "piecewiseRenderBlockEntities", at = @At("HEAD"), argsOnly = true)
	private SubmitNodeStorage litematicapass$routeBlockEntities(SubmitNodeStorage queue) {
		return SchematicEntities.route(queue);
	}
	//?} else {
	/*// 实体和方块实体画进我们自己的 buffer source，再冲到我们的目标里（见 SchematicEntities）。
	@WrapMethod(method = "piecewiseRenderEntities")
	private void litematicapass$routeEntities(PoseStack matrices, MultiBufferSource.BufferSource immediate, float partialTicks, ProfilerFiller profiler,
		Operation<Void> original) {
		if (SchematicTarget.isActive()) {
			SchematicEntities.drawInto(source -> original.call(matrices, source, partialTicks, profiler));
		} else {
			original.call(matrices, immediate, partialTicks, profiler);
		}
	}

	@WrapMethod(method = "piecewiseRenderBlockEntities")
	private void litematicapass$routeBlockEntities(PoseStack matrices, MultiBufferSource.BufferSource immediate, MultiBufferSource.BufferSource effects,
		float partialTicks, ProfilerFiller profiler, Operation<Void> original) {
		if (SchematicTarget.isActive()) {
			SchematicEntities.drawInto(source -> original.call(matrices, source, source, partialTicks, profiler));
		} else {
			original.call(matrices, immediate, effects, partialTicks, profiler);
		}
	}
	*///?}

	@Inject(method = "piecewiseRenderOverlay", at = @At("HEAD"))
	private void litematicapass$beginOverlay(CallbackInfo ci) {
		SchematicTarget.begin();
		// Before the overlay boxes, which would hide the entities submitted after the terrain.
		if (SchematicTarget.isRouting()) {
			SchematicTarget.drawPendingEntities();
		}
	}

	@Inject(method = "piecewiseRenderOverlay", at = @At("RETURN"))
	private void litematicapass$endOverlay(CallbackInfo ci) {
		SchematicTarget.end();
	}
}
