package ml.mypals.litematicapass.mixin.iris;

import org.spongepowered.asm.mixin.Mixin;
//? if <1.18.2 {
/*import net.coderbot.iris.rendertarget.RenderTargets;
import org.spongepowered.asm.mixin.gen.Accessor;
*///?}

/**
 * Iris 1.2 及更早（Minecraft 1.18.1 及更早）把场景深度放在自己的深度纹理里，而不是主目标的深度里（见 IrisHooks）。
 * 只在这些版本上应用（见 LitematicaPassMixinPlugin）。
 */
@Mixin(targets = "net.coderbot.iris.pipeline.newshader.NewWorldRenderingPipeline", remap = false)
public interface LegacyIrisPipelineAccessor {
	//? if <1.18.2 {
	/*@Accessor("renderTargets")
	RenderTargets litematicapass$renderTargets();
	*///?}
}
