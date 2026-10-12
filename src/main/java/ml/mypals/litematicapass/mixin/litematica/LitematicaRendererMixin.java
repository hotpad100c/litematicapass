package ml.mypals.litematicapass.mixin.litematica;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import ml.mypals.litematicapass.compat.IrisCompat;
import ml.mypals.litematicapass.render.SchematicEntities;
import ml.mypals.litematicapass.render.SchematicTarget;
//? if >=1.21 {
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.profiling.ProfilerFiller;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 将 schematic 地形、每个区块的网格叠加层、实体和方块实体重定向到我们的目标中。 */
@Mixin(value = LitematicaRenderer.class, remap = false)
public class LitematicaRendererMixin {
	// Litematica 0.22 and older (Minecraft 1.21.5 and older) draw each terrain layer into the main target, which
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

	// 实体和方块实体画进我们自己的 buffer source，再冲到我们的目标里（见 SchematicEntities）。
	//? if >=1.21 {
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
	//?} else {
	/*// Before 1.21 Litematica draws entities and block entities in one call (its parameters vary between builds), into
	// Minecraft's buffer source, which WorldRendererSchematicMixin swaps for ours in between.
	@Inject(method = "piecewiseRenderEntities", at = @At("HEAD"))
	private void litematicapass$beginEntities(CallbackInfo ci) {
		SchematicEntities.begin();
	}

	@Inject(method = "piecewiseRenderEntities", at = @At("RETURN"))
	private void litematicapass$endEntities(CallbackInfo ci) {
		SchematicEntities.end();
	}
	*///?}

	@Inject(method = "piecewiseRenderOverlay", at = @At("HEAD"))
	private void litematicapass$beginOverlay(CallbackInfo ci) {
		SchematicTarget.begin();
	}

	@Inject(method = "piecewiseRenderOverlay", at = @At("RETURN"))
	private void litematicapass$endOverlay(CallbackInfo ci) {
		SchematicTarget.end();
	}

	//? if <1.18.2 {
	/*// Iris 1.2 and older (Minecraft 1.18.1 and older) draw the translucent layer again in their shadow pass, through the
	// vanilla method Litematica draws its translucent blocks and overlay from. Litematica prepares the schematic once a
	// frame, before that shadow pass, and the cleanup after its overlay would leave the main pass with nothing to draw.
	@Inject(method = "cleanup", at = @At("HEAD"), cancellable = true)
	private void litematicapass$keepForMainPass(CallbackInfo ci) {
		if (IrisCompat.isRenderingShadowPass()) {
			ci.cancel();
		}
	}
	*///?}
}
