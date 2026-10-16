package ml.mypals.litematicapass.compat;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
//? if >=26.2 {
import ml.mypals.litematicapass.LitematicaPass;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import org.joml.Matrix4fc;
//?}
import net.irisshaders.iris.vertices.ImmediateState;

final class IrisHooks {
	static boolean isPackInUse() {
		return Iris.isPackInUseQuick();
	}
	static boolean isRenderingShadowPass() {
		return IrisApi.getInstance().isRenderingShadowPass();
	}
	//? if >=1.21.4 {
	static boolean bypass() {
		return ImmediateState.bypass;
	}
	static void setBypass(boolean bypass) {
		ImmediateState.bypass = bypass;
	}
	//?} else {
	/*// Iris 1.8.1 and older (Minecraft 1.21.3 and older) have no bypass switch; IrisShaderOverrideMixin reads ours.
	private static boolean bypass;

	static boolean bypass() {
		return bypass;
	}
	static void setBypass(boolean bypass) {
		IrisHooks.bypass = bypass;
	}
	*///?}
	//? if >=26.3 {
	static boolean skipExtension() {
		return ImmediateState.skipExtension.get();
	}
	static void setSkipExtension(boolean skip) {
		ImmediateState.skipExtension.set(skip);
	}
	//?} else {
	/*// Iris for Minecraft 26.2 and older already hands out its extended terrain format as the vanilla terrain format,
	// so the schematic's buffers and the vanilla pipelines agree on it; skipping the extension there leaves the buffers
	// in that format without its extra data ("Missing elements in vertex: mc_midTexCoord, at_tangent"). Leave it alone.
	static boolean skipExtension() {
		return false;
	}
	static void setSkipExtension(boolean skip) {
	}
	*///?}

	/**
	 * Whether the schematic's chunk meshes must be built in Iris' widened terrain format. 26.3 and newer have the
	 * extension switch above and use it to keep the meshes on the vanilla format. Iris for 26.2 widens every terrain
	 * binding while the level renders, with nothing to opt out of it, so the meshes have to be widened to match; the
	 * Iris versions of the other Minecraft versions were not checked and keep whatever they did before.
	 */
	static boolean schematicMeshesWidened() {
		//? if >=26.2 && <26.3 {
		/*return true;
		*///?} else {
		return false;
		//?}
	}

	//? if >=26.2 {
	private static Boolean packUsesReversedDepth;

	/**
	 * Whether the shader pack renders the world with reversed Z (near = 1, far = 0). Iris up to 1.11.2 undoes the
	 * reversed Z of Minecraft 26.2 for the whole frame, packs included, so the depth it writes is standard Z.
	 */
	static boolean packUsesReversedDepth() {
		if (packUsesReversedDepth == null) {
			try {
				Matrix4fc projection = CapturedRenderingState.INSTANCE.getGbufferProjection();
				packUsesReversedDepth = projection != null && projection.m22() > 0.0F;
			} catch (LinkageError | RuntimeException e) {
				// Iris' internals may be missing or renamed; fall back to Minecraft's own convention.
				packUsesReversedDepth = false;
			}
			LitematicaPass.LOGGER.info("Iris renders the world in {} Z", packUsesReversedDepth ? "reversed" : "standard");
		}
		return packUsesReversedDepth;
	}
	//?} else {
	/*// Minecraft before 26.2 is standard Z (near = 0) and Iris does not undo anything there.
	static boolean packUsesReversedDepth() {
		return false;
	}
	*///?}

	static boolean isRenderingLevel() {
		return ImmediateState.isRenderingLevel;
	}

	static void setRenderingLevel(boolean renderingLevel) {
		ImmediateState.isRenderingLevel = renderingLevel;
	}
}
