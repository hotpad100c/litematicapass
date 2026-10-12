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
	static boolean bypass() {
		return ImmediateState.bypass;
	}
	static void setBypass(boolean bypass) {
		ImmediateState.bypass = bypass;
	}
	static boolean skipExtension() {
		return ImmediateState.skipExtension.get();
	}
	static void setSkipExtension(boolean skip) {
		ImmediateState.skipExtension.set(skip);
	}

	static boolean isRenderingLevel() {
		return ImmediateState.isRenderingLevel;
	}

	static void setRenderingLevel(boolean renderingLevel) {
		ImmediateState.isRenderingLevel = renderingLevel;
	}
}
