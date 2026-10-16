package ml.mypals.litematicapass.render;

import litematica.config.Configs;
import ml.mypals.litematicapass.LitematicaPass;
import org.lwjgl.opengl.GL30;

/**
 * Our own copies of the shader pack's scene depth at three moments of OptiFine's frame, taken from the depth of its
 * framebuffer ({@code dfb}) while it is bound: just before the first-person hand is drawn, just before translucent
 * surfaces are drawn (after the hand), and before the composite passes (everything). They are the equivalents of the
 * pack's {@code depthtex2}, {@code depthtex1} and {@code depthtex0}, without reaching into OptiFine's internals; see
 * {@code ShadersMixin} and {@code ShadersRenderMixin} for the capture points.
 *
 * <p>Unlike Iris, OptiFine never shares its scene depth with Minecraft's framebuffer, so the newer versions' "main
 * depth" is the third copy here, and the depth the schematic is seeded from is OptiFine's depth texture itself
 * ({@link #live}).
 */
public final class SceneDepth {
	private static final Gpu.Texture NO_HAND = new Gpu.Texture(0, 0, 0);
	private static final Gpu.Texture OPAQUE = new Gpu.Texture(0, 0, 0);
	private static final Gpu.Texture FINAL = new Gpu.Texture(0, 0, 0);
	private static boolean noHandReady;
	private static boolean opaqueReady;
	private static boolean finalReady;
	private static Gpu.Texture live;
	private static boolean warned;

	private SceneDepth() {}

	/** Only copy when the schematic will actually be drawn this frame. */
	private static boolean wanted() {
		return SchematicTarget.isActive() && Configs.Visuals.MAIN_RENDERING_TOGGLE.getBooleanValue();
	}

	/** The scene depth before the first-person hand is drawn (OptiFine draws the opaque hand before translucents). */
	public static void captureBeforeHand() {
		if (wanted()) {
			noHandReady = capture(NO_HAND);
		}
	}

	/** The scene depth after the solid phase (hand included) and before translucent surfaces are drawn. */
	public static void captureBeforeTranslucents() {
		if (wanted()) {
			opaqueReady = capture(OPAQUE);
		}
	}

	/** The final scene depth, with translucent surfaces (and a translucent held item), before the composite passes. */
	public static void captureFinal() {
		if (wanted()) {
			finalReady = capture(FINAL);
		}
	}

	private static boolean capture(Gpu.Texture copy) {
		Gpu.Texture source = Gpu.boundDepthTexture(GL30.GL_READ_FRAMEBUFFER);
		if (source == null) {
			if (!warned) {
				warned = true;
				LitematicaPass.LOGGER.warn("The shader pack's framebuffer has no depth texture where expected; the schematic may show through the scene");
			}
			return false;
		}
		live = source;
		Gpu.copyBoundDepth(source, copy);
		return true;
	}

	/**
	 * OptiFine's scene depth texture, from the framebuffer bound for drawing now (its {@code dfb} while the world is
	 * drawn), or the one last seen; null if none was seen.
	 */
	static Gpu.Texture live() {
		Gpu.Texture bound = Gpu.boundDepthTexture(GL30.GL_DRAW_FRAMEBUFFER);
		if (bound != null) {
			live = bound;
		}
		return live;
	}

	/** Depth without translucent surfaces: the copy if it was captured this frame, else the live depth. */
	static Gpu.Texture opaque() {
		return opaqueReady ? OPAQUE : live;
	}

	/** Depth without translucent surfaces and without the hand; falls back like {@link #opaque()}. */
	static Gpu.Texture noHand() {
		return noHandReady ? NO_HAND : opaque();
	}

	/** The final depth, with translucent surfaces; falls back like {@link #opaque()}. */
	static Gpu.Texture main() {
		return finalReady ? FINAL : opaque();
	}

	static void endFrame() {
		noHandReady = false;
		opaqueReady = false;
		finalReady = false;
	}
}
