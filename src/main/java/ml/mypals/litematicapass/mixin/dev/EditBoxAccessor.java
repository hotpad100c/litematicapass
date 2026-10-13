package ml.mypals.litematicapass.mixin.dev;

import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 仅开发环境（见 MalilibTextFieldDevMixin）。 */
@Mixin(EditBox.class)
public interface EditBoxAccessor {
	@Accessor("cursorPos")
	int litematicapass$getCursorPos();

	@Accessor("cursorPos")
	void litematicapass$setCursorPos(int cursorPos);
}
