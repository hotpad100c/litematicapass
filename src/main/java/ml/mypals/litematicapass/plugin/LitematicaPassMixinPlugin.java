package ml.mypals.litematicapass.plugin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class LitematicaPassMixinPlugin implements IMixinConfigPlugin {
	private static final String PACKAGE = "ml.mypals.litematicapass.mixin.";

	@Override
	public void onLoad(String mixinPackage) {
		if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
			Mixins.registerErrorHandlerClass(DevMixinErrorHandler.class.getName());
		}
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		String name = mixinClassName.startsWith(PACKAGE) ? mixinClassName.substring(PACKAGE.length()) : mixinClassName;
		if (name.startsWith("iris.")) {
			// Iris is optional. IrisShaderOverrideMixin keeps Iris from wanting to override shaders while routing: Iris
			// would otherwise lock colour and depth writes off for the vanilla shaders the schematic draws with.
			return FabricLoader.getInstance().isModLoaded("iris");
		}
		if (name.equals("litematica.WorldRendererSchematicMixin")) {
			// From 1.21 Litematica takes the entities' buffer source as a parameter (see LitematicaRendererMixin).
			//? if >=1.21 {
			return false;
			//?} else {
			/*return true;
			*///?}
		}
		return true;
	}
}
