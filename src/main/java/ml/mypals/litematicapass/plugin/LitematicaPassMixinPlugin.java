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
			if (name.equals("iris.IrisShaderOverrideMixin")) {
				// Only Iris builds without their own bypass switch need it.
				//? if >=1.21.4 {
				return false;
				//?}
			}
			return FabricLoader.getInstance().isModLoaded("iris");
		}
		if (name.equals("litematica.LegacyTerrainFixUniformMixin") || name.equals("litematica.ChunkRenderBatchDrawMixin")
			|| name.equals("StrictValidationMixin")) {
			// Mixin rejects the whole config over a missing target class, and older Litematica / Minecraft builds lack these.
			if (LitematicaPassMixinPlugin.class.getClassLoader().getResource(targetClassName.replace('.', '/') + ".class") == null) {
				return false;
			}
		}
		if (name.equals("MinecraftMixin") || name.equals("MinecraftAccessor")) {
			// Only versions without RenderSystem's output target overrides route through the main target.
			//? if >=1.21.6 {
			return false;
			//?} else {
			/*return true;
			*///?}
		}
		if (name.equals("GlStateManagerMixin")) {
			// Only the OpenGL-state versions blend through GlStateManager.
			//? if >=1.21.5 {
			return false;
			//?} else {
			/*return true;
			*///?}
		}
		if (name.equals("litematica.RenderUtilsMixin")) {
			// Before 1.21.6 MinecraftMixin already routes the main target that malilib's accessor returns.
			//? if >=1.21.6 {
			return true;
			//?} else {
			/*return false;
			*///?}
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
