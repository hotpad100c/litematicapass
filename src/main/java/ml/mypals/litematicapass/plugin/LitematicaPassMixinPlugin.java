package ml.mypals.litematicapass.plugin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class LitematicaPassMixinPlugin implements IMixinConfigPlugin {
	private static final String PACKAGE = "ml.mypals.litematicapass.mixin.";

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		String name = mixinClassName.startsWith(PACKAGE) ? mixinClassName.substring(PACKAGE.length()) : mixinClassName;
		if (name.startsWith("iris.")) {
			return FabricLoader.getInstance().isModLoaded("iris");
		}
		return true;
	}
}
