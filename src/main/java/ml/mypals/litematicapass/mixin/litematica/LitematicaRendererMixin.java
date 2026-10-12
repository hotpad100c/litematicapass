package ml.mypals.litematicapass.mixin.litematica;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import ml.mypals.litematicapass.render.SchematicEntities;
import ml.mypals.litematicapass.render.SchematicTarget;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;

/** 将 schematic 地形、每个区块的网格叠加层、实体和方块实体重定向到我们的目标中。 */
@Mixin(value = LitematicaRenderer.class, remap = false)
public class LitematicaRendererMixin {
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

	/** 实体和方块实体提交到我们自己的 SubmitNodeStorage，而不是原版的。 */
	@WrapMethod(method = "piecewiseRenderEntities")
	private void litematicapass$routeEntities(PoseStack matrices, LevelRenderState renderStates, SubmitNodeCollector queue, ProfilerFiller profiler,
		Operation<Void> original) {
		original.call(matrices, renderStates, SchematicEntities.route(queue), profiler);
	}

	@WrapMethod(method = "piecewiseRenderBlockEntities")
	private void litematicapass$routeBlockEntities(PoseStack matrices, LevelRenderState renderStates, SubmitNodeStorage queue, ProfilerFiller profiler,
		Operation<Void> original) {
		original.call(matrices, renderStates, SchematicEntities.route(queue), profiler);
	}

	@WrapMethod(method = "piecewiseRenderOverlay")
	private void litematicapass$routeOverlay(ProfilerFiller profiler, Operation<Void> original) {
		SchematicTarget.begin();
		try {
			original.call(profiler);
		} finally {
			SchematicTarget.end();
		}
	}
}
