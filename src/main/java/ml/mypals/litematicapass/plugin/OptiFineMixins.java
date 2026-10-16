package ml.mypals.litematicapass.plugin;

import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.Mixins;

/**
 * Registers the mixins into OptiFine's classes once OptiFine is on the classpath. Legacy OptiFabric only adds it from
 * its own {@code mm:early_risers} entrypoint, after the mixin configs named in fabric.mod.json are prepared, and Mixin
 * drops a {@code @Pseudo} target it cannot find at that point ("Skipping virtual target"). Configurations added here
 * are prepared later, when OptiFine's classes can be found (OptiFabric registers its own OptiFine mixins the same way).
 * The {@code mm:early_risers} entrypoint only runs when a mod brings Fabric ASM, as OptiFabric does.
 */
public final class OptiFineMixins implements Runnable {
	private static boolean registered;

	/** Whether the OptiFine mixin configuration was registered (for the debug status). */
	public static boolean registered() {
		return registered;
	}

	@Override
	public void run() {
		if (FabricLoader.getInstance().isModLoaded("optifabric")) {
			Mixins.addConfiguration("litematicapass.optifine.mixins.json");
			registered = true;
		}
	}
}
