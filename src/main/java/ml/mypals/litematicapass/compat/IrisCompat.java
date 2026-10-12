package ml.mypals.litematicapass.compat;

import net.fabricmc.loader.api.FabricLoader;

public final class IrisCompat {
	public static final boolean LOADED = FabricLoader.getInstance().isModLoaded("iris");
	public static boolean isPackInUse() {
		return LOADED && IrisHooks.isPackInUse();
	}
	public static boolean isRenderingShadowPass() {
		return LOADED && IrisHooks.isRenderingShadowPass();
	}
	public static boolean bypass() {
		return LOADED && IrisHooks.bypass();
	}
	public static void setBypass(boolean bypass) {
		if (LOADED) {
			IrisHooks.setBypass(bypass);
		}
	}

	/** Iris only widens vertex formats while the level is being rendered. */
	public static boolean isRenderingLevel() {
		return LOADED && IrisHooks.isRenderingLevel();
	}

	public static void setRenderingLevel(boolean renderingLevel) {
		if (LOADED) {
			IrisHooks.setRenderingLevel(renderingLevel);
		}
	}

	/**
	 * The texture the shader pack's scene depth is in when it is not the main depth (Iris 1.2 and older), else 0.
	 */
	public static int sceneDepthTexture() {
		return LOADED ? IrisHooks.sceneDepthTexture() : 0;
	}
}
