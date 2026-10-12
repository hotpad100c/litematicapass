package ml.mypals.litematicapass.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import fi.dy.masa.litematica.config.Configs;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;

/**
 * Our own copies of the scene depth at two moments of the shader pack's frame, taken from the main depth (which the
 * pack shares): just before the first-person hand is drawn, and just before translucent surfaces are drawn (after
 * the hand). They are the equivalents of the pack's {@code depthtex2} and {@code depthtex1}, without reaching into
 * Iris's internals; see {@code IrisRenderingPipelineMixin} for the capture points.
 */
public final class SceneDepth {
	private static TextureTarget noHand;
	private static TextureTarget opaque;
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

	private static TextureTarget copy(@Nullable TextureTarget target, String label) {
		RenderTarget main = Minecraft.getInstance().gameRenderer.mainRenderTarget();
		int width = Math.max(1, main.width);
		int height = Math.max(1, main.height);
		if (target == null) {
			target = new TextureTarget(label, width, height, null, GpuFormat.D32_FLOAT);
		} else if (target.width != width || target.height != height) {
			target.resize(width, height);
		}
		target.copyDepthFrom(main);
		return target;
	}

	/** Depth without translucent surfaces, or null if it was not captured this frame. */
	@Nullable
	static GpuTextureView opaqueView() {
		return opaqueReady && opaque != null ? opaque.getDepthTextureView() : null;
	}

	/** Depth without translucent surfaces and without the hand, or null if it was not captured this frame. */
	@Nullable
	static GpuTextureView noHandView() {
		return noHandReady && noHand != null ? noHand.getDepthTextureView() : null;
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
