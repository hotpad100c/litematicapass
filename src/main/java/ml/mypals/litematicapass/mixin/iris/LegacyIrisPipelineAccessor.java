package ml.mypals.litematicapass.mixin.iris;

import org.spongepowered.asm.mixin.Mixin;
//? if <1.18.2 {
/*import net.coderbot.iris.rendertarget.RenderTargets;
import org.spongepowered.asm.mixin.gen.Accessor;
*///?}

/**
 * Minecraft 1.18.1 及更早的 Iris 把场景深度放在自己的深度纹理里，而不是主目标的深度里（见 IrisHooks）。
 * 只在这些版本上应用（见 LitematicaPassMixinPlugin）。
 */
//? if >=1.17 {
@Mixin(targets = "net.coderbot.iris.pipeline.newshader.NewWorldRenderingPipeline", remap = false)
//?} else {
/*@Mixin(targets = "net.coderbot.iris.pipeline.DeferredWorldRenderingPipeline", remap = false)
*///?}
public interface LegacyIrisPipelineAccessor {
	//? if <1.18.2 {
	/*@Accessor("renderTargets")
	RenderTargets litematicapass$renderTargets();
	*///?}
}
