package ml.mypals.litematicapass.mixin.optifine;

import ml.mypals.litematicapass.compat.OptiFineCompat;
import ml.mypals.litematicapass.render.DeferredSchematic;
import ml.mypals.litematicapass.render.SceneDepth;
import ml.mypals.litematicapass.render.SchematicTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Pseudo
@Mixin(targets = "net.optifine.shaders.Shaders", remap = false)
public class ShadersMixin {
	// OptiFine copies its depthtex1 here, from its framebuffer, which is still bound. The opaque scene is complete:
	// draw the schematic's solid, cutout and entities, held back until now (DeferredSchematic).
	@Inject(method = "preWater", at = @At("HEAD"), require = 0)
	private static void litematicapass$captureBeforeTranslucents(CallbackInfo ci) {
		SceneDepth.captureBeforeTranslucents();
		DeferredSchematic.replay();
	}

	@Inject(method = "renderCompositeFinal", at = @At("HEAD"), require = 0)
	private static void litematicapass$captureFinal(CallbackInfo ci) {
		SceneDepth.captureFinal();
	}

	// The final pass has written the shaded frame into Minecraft's framebuffer: merge the schematic onto it, before the
	// first-person overlays OptiFine draws next.
	@Inject(method = "renderCompositeFinal", at = @At("RETURN"), require = 0)
	private static void litematicapass$compositeSchematic(CallbackInfo ci) {
		SchematicTarget.compositeAndReset();
	}

	// While routing, the schematic is drawn with Minecraft's fixed-function rendering into our framebuffer: OptiFine
	// must not bind its programs (which write its G-buffers) or point the draw buffers at attachments ours lacks.
	@Inject(method = "useProgram", at = @At("HEAD"), cancellable = true, require = 0)
	private static void litematicapass$keepProgram(CallbackInfo ci) {
		if (OptiFineCompat.bypass()) {
			ci.cancel();
		}
	}

	@Inject(method = "setDrawBuffers", at = @At("HEAD"), cancellable = true, require = 0)
	private static void litematicapass$keepDrawBuffers(CallbackInfo ci) {
		if (OptiFineCompat.bypass()) {
			ci.cancel();
		}
	}
}
