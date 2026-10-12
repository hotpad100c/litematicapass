package ml.mypals.litematicapass.compat;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
//? if >=1.18.2 {
import net.irisshaders.iris.vertices.ImmediateState;
//?} else {
/*import ml.mypals.litematicapass.mixin.iris.LegacyIrisPipelineAccessor;
*///?}

final class IrisHooks {
	static boolean isPackInUse() {
		//? if >=1.21 {
		return Iris.isPackInUseQuick();
		//?} else {
		/*return IrisApi.getInstance().isShaderPackInUse();
		*///?}
	}
	static boolean isRenderingShadowPass() {
		return IrisApi.getInstance().isRenderingShadowPass();
	}
	// Iris 1.8.1 and older (Minecraft 1.21.3 and older) have no bypass switch; IrisShaderOverrideMixin reads ours.
	private static boolean bypass;

	static boolean bypass() {
		return bypass;
	}
	static void setBypass(boolean bypass) {
		IrisHooks.bypass = bypass;
	}
	//? if >=1.18.2 {
	static boolean isRenderingLevel() {
		return ImmediateState.isRenderingLevel;
	}

	static void setRenderingLevel(boolean renderingLevel) {
		ImmediateState.isRenderingLevel = renderingLevel;
	}

	static int sceneDepthTexture() {
		return 0;
	}
	//?} else {
	/*// Iris 1.2 and older (Minecraft 1.18.1 and older) have no such switch: they never widen the entity formats.
	static boolean isRenderingLevel() {
		return false;
	}

	static void setRenderingLevel(boolean renderingLevel) {
	}

	// They keep the scene depth in their own texture. While routing, IrisShaderOverrideMixin hides the pipeline.
	static int sceneDepthTexture() {
		boolean routing = bypass;
		bypass = false;
		try {
			return Iris.getPipelineManager().getPipelineNullable() instanceof LegacyIrisPipelineAccessor pipeline
				? pipeline.litematicapass$renderTargets().getDepthTexture().getTextureId() : 0;
		} finally {
			bypass = routing;
		}
	}
	*///?}
}
