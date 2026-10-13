package ml.mypals.litematicapass.mixin.iris;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import ml.mypals.litematicapass.compat.IrisCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
//? if <1.17 {
/*import org.spongepowered.asm.mixin.injection.ModifyVariable;
*///?}

//? if >=1.18.2 {
@Mixin(value = net.irisshaders.iris.pipeline.IrisRenderingPipeline.class, remap = false)
public class IrisShaderOverrideMixin {
	@ModifyReturnValue(method = "shouldOverrideShaders", at = @At("RETURN"))
	private boolean litematicapass$bypass(boolean original) {
		return original && !IrisCompat.bypass();
	}

	// Iris picks its block entity (and hand) shaders by the phase before asking shouldOverrideShaders, and Litematica
	// draws the schematic's block entities during the block entity phase.
	@ModifyReturnValue(method = "getPhase", at = @At("RETURN"))
	private net.irisshaders.iris.pipeline.WorldRenderingPhase litematicapass$noPhase(net.irisshaders.iris.pipeline.WorldRenderingPhase original) {
		return IrisCompat.bypass() ? net.irisshaders.iris.pipeline.WorldRenderingPhase.NONE : original;
	}
}
//?} else if >=1.17 {
/*// Iris 1.2 and older (Minecraft 1.17 to 1.18.1) replace the terrain shaders whenever a pipeline exists, without
// asking it; while routing, Iris finds no pipeline.
@Mixin(value = net.coderbot.iris.pipeline.PipelineManager.class, remap = false)
public class IrisShaderOverrideMixin {
	@ModifyReturnValue(method = "getPipelineNullable", at = @At("RETURN"))
	private net.coderbot.iris.pipeline.WorldRenderingPipeline litematicapass$bypass(net.coderbot.iris.pipeline.WorldRenderingPipeline original) {
		return IrisCompat.bypass() ? null : original;
	}
}
*///?} else {
/*// Before 1.17 (fixed-function rendering) Iris only binds its programs while it believes the main target is bound;
// while routing, it is not (IrisHooks.setBypass tells it so as well).
@Mixin(value = net.irisshaders.iris.pipeline.IrisRenderingPipeline.class, remap = false)
public class IrisShaderOverrideMixin {
	@ModifyVariable(method = "setIsMainBound", at = @At("HEAD"), argsOnly = true)
	private boolean litematicapass$bypass(boolean mainBound) {
		return mainBound && !IrisCompat.bypass();
	}
}
*///?}
