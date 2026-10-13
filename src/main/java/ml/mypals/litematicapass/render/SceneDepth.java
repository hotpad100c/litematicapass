package ml.mypals.litematicapass.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import fi.dy.masa.litematica.config.Configs;
import ml.mypals.litematicapass.compat.IrisCompat;
import org.jetbrains.annotations.Nullable;

/**
 * Our own copies of the scene depth at two moments of the shader pack's frame, taken from the main depth (which the
 * pack shares): just before the first-person hand is drawn, and just before translucent surfaces are drawn (after
 * the hand). They are the equivalents of the pack's {@code depthtex2} and {@code depthtex1}, without reaching into
 * Iris's internals; see {@code IrisRenderingPipelineMixin} for the capture points.
 */
public final class SceneDepth {
	private static RenderTarget noHand;
	private static RenderTarget opaque;
	private static boolean noHandReady;
	private static boolean opaqueReady;

	private SceneDepth() {}

	/** Only copy when the schematic will actually be drawn this frame. */
	private static boolean wanted() {
		return SchematicTarget.isActive() && Configs.Visuals.ENABLE_RENDERING.getBooleanValue();
	}

	/** The scene depth before the first-person hand is drawn. */
	public static void captureBeforeHand() {
		if (wanted()) {
			noHand = copy(noHand, "litematicapass_depth_no_hand");
			noHandReady = true;
		}
	}

	/** The scene depth after the solid phase (hand included) and before translucent surfaces are drawn. */
	public static void captureBeforeTranslucents() {
		if (wanted()) {
			opaque = copy(opaque, "litematicapass_depth_opaque");
			opaqueReady = true;
		}
	}

	/**
	 * Brings the shader pack's scene depth into the main depth where they are not the same (Iris 1.2 and older keep it
	 * in their own texture); call before reading the main depth.
	 */
	static void syncMainDepth() {
		int texture = IrisCompat.sceneDepthTexture();
		RenderTarget main = Gpu.mainTarget();
		if (texture != 0 && texture != main.getDepthTextureId()) {
			Gpu.copyDepthTexture(texture, main);
		}
	}

	private static RenderTarget copy(@Nullable RenderTarget target, String label) {
		syncMainDepth();
		RenderTarget main = Gpu.mainTarget();
		int width = Math.max(1, main.width);
		int height = Math.max(1, main.height);
		if (target == null) {
			target = Gpu.depthTarget(label, width, height);
		} else if (target.width != width || target.height != height) {
			Gpu.resize(target, width, height);
		}
		Gpu.copyDepth(target, main);
		return target;
	}

	/** Depth without translucent surfaces, or null if it was not captured this frame. */
	@Nullable
	static RenderTarget opaque() {
		return opaqueReady ? opaque : null;
	}

	/** Depth without translucent surfaces and without the hand, or null if it was not captured this frame. */
	@Nullable
	static RenderTarget noHand() {
		return noHandReady ? noHand : null;
	}

	static void endFrame() {
		noHandReady = false;
		opaqueReady = false;
	}

	static void close() {
		if (noHand != null) {
			noHand.destroyBuffers();
			noHand = null;
		}
		if (opaque != null) {
			opaque.destroyBuffers();
			opaque = null;
		}
		endFrame();
	}
}
