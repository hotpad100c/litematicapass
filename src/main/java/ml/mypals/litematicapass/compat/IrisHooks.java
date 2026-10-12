package ml.mypals.litematicapass.compat;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
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

	static boolean isRenderingLevel() {
		return ImmediateState.isRenderingLevel;
	}

	static void setRenderingLevel(boolean renderingLevel) {
		ImmediateState.isRenderingLevel = renderingLevel;
	}
}
