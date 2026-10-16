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
	public static boolean skipExtension() {
		return LOADED && IrisHooks.skipExtension();
	}
	public static void setSkipExtension(boolean skip) {
		if (LOADED) {
			IrisHooks.setSkipExtension(skip);
		}
	}

	/** Whether the schematic's chunk meshes must be built in Iris' widened terrain format. */
	public static boolean schematicMeshesWidened() {
		return LOADED && IrisHooks.schematicMeshesWidened();
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

	/** Whether the world depth the pack writes is reversed Z (near = 1); the screen shaders must match it. */
	public static boolean packUsesReversedDepth() {
		return LOADED && IrisHooks.packUsesReversedDepth();
	}
}
