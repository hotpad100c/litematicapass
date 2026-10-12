package ml.mypals.litematicapass.smoke.mixin;

import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
//? if <1.19.4 {
/*import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.gen.Accessor;
*///?}

/** Presses "Create New World" without the mouse (development check only, see SmokeTest). */
@Mixin(CreateWorldScreen.class)
public interface CreateWorldScreenAccessor {
	@Invoker("onCreate")
	void litematicapass$onCreate();

	//? if <1.19.4 {
	/*// Before 1.19.4 (no WorldCreationUiState) the screen keeps the name and game mode itself; the game mode type is
	// package-private, so SmokeTest presses its button instead.
	@Accessor("nameEdit")
	EditBox litematicapass$nameEdit();

	@Accessor("modeButton")
	CycleButton<?> litematicapass$modeButton();
	*///?}
}
