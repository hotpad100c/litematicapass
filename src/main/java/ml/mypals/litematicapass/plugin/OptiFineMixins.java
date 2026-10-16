package ml.mypals.litematicapass.plugin;

import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.Mixins;


public final class OptiFineMixins implements Runnable {
	private static boolean registered;

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
