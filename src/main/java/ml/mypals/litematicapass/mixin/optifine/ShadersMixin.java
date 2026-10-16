package ml.mypals.litematicapass.mixin.optifine;

import ml.mypals.litematicapass.compat.OptiFineCompat;
import ml.mypals.litematicapass.render.SceneDepth;
import ml.mypals.litematicapass.render.SchematicTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * OptiFine 的光影管线（net.optifine.shaders.Shaders，OptiFine 1.12.2 HD U G5）。EntityRenderer.renderWorldPass 里的顺序：
 * 固体/镂空地形 → 实体 → ShadersRender.renderHand0（不透明手部）→ preWater（复制 depthtex1）→ 半透明（水）
 * → ShadersRender.renderHand1（半透明手部）→ renderCompositeFinal（composite 与 final 通道，写入 Minecraft 的帧缓冲）。
 * Litematica 的所有绘制（包括 malilib 的 world-last 事件）都在 renderCompositeFinal 之前。
 * （类名写成字符串：OptiFine 不是编译依赖，运行时由 Legacy OptiFabric 加载。）
 */
@Pseudo
@Mixin(targets = "net.optifine.shaders.Shaders", remap = false)
public class ShadersMixin {
	// OptiFine copies its depthtex1 here, from its framebuffer, which is still bound.
	@Inject(method = "preWater", at = @At("HEAD"), require = 0)
	private static void litematicapass$captureBeforeTranslucents(CallbackInfo ci) {
		SceneDepth.captureBeforeTranslucents();
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
