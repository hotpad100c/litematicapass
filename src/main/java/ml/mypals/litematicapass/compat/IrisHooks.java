package ml.mypals.litematicapass.compat;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
//? if >=1.18.2 || <1.17 {
import net.irisshaders.iris.vertices.ImmediateState;
//?}
//? if <1.18.2 {
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
		//? if <1.17 {
		/*// Iris for fixed-function rendering only notices which target is bound when one is bound for writing.
		Iris.getPipelineManager().getPipeline().ifPresent(pipeline -> pipeline.getRenderTargetStateListener().setIsMainBound(!bypass));
		*///?}
	}
	//? if >=1.18.2 || <1.17 {
	static boolean isRenderingLevel() {
		return ImmediateState.isRenderingLevel;
	}

	static void setRenderingLevel(boolean renderingLevel) {
		ImmediateState.isRenderingLevel = renderingLevel;
	}
	//?} else {
	/*// Iris 1.2 and older for Minecraft 1.17 to 1.18.1 have no such switch: they never widen the entity formats.
	static boolean isRenderingLevel() {
		return false;
	}

	static void setRenderingLevel(boolean renderingLevel) {
	}
	*///?}

	//? if >=1.18.2 {
	static int sceneDepthTexture() {
		return 0;
	}
	//?} else {
	/*// Iris for Minecraft 1.18.1 and older keeps the scene depth in its own texture. While routing,
	// IrisShaderOverrideMixin may hide the pipeline.
	static int sceneDepthTexture() {
		boolean routing = bypass;
		bypass = false;
		try {
			if (!(Iris.getPipelineManager().getPipelineNullable() instanceof LegacyIrisPipelineAccessor pipeline)) {
				return 0;
			}
			// Most builds hand out their DepthTexture, some (the later ones for 1.17.1, the one for 1.16.5) its ID.
			Object depth = pipeline.litematicapass$renderTargets().getDepthTexture();
			return depth instanceof Integer id ? id : textureId(depth);
		} finally {
			bypass = routing;
		}
	}

	private static java.lang.reflect.Method getTextureId;

	private static int textureId(Object depthTexture) {
		try {
			if (getTextureId == null) {
				getTextureId = depthTexture.getClass().getMethod("getTextureId");
			}
			return (int) getTextureId.invoke(depthTexture);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Unexpected Iris depth texture " + depthTexture, e);
		}
	}
	*///?}
}
