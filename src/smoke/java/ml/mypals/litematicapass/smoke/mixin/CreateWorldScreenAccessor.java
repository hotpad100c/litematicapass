package ml.mypals.litematicapass.smoke.mixin;

import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Presses "Create New World" without the mouse (development check only, see SmokeTest). */
@Mixin(CreateWorldScreen.class)
public interface CreateWorldScreenAccessor {
	@Invoker("onCreate")
	void litematicapass$onCreate();
}
