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
		if (name.equals("litematica.LegacyTerrainFixUniformMixin")) {
			// Mixin rejects the whole config over a missing target class, and older Litematica builds lack this one.
			return LitematicaPassMixinPlugin.class.getClassLoader().getResource(targetClassName.replace('.', '/') + ".class") != null;
		}
		if (name.equals("litematica.ChunkRenderBatchDrawMixin")) {
			// Only Litematica builds that choose the terrain output target themselves need it.
			//? if >=26.3 {
			return false;
			//?} else {
			/*return true;
			*///?}
		}
		return true;
	}
}
