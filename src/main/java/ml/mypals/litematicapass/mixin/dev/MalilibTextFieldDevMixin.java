package ml.mypals.litematicapass.mixin.dev;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;


@Mixin(value = GuiTextFieldGeneric.class, remap = false)
public class MalilibTextFieldDevMixin {
	@Unique
	private boolean litematicapass$moving;

	@WrapMethod(method = "setCursorPosition", require = 0)
	private void litematicapass$breakLoop(int pos, Operation<Void> original) {
		if (this.litematicapass$moving) {
			((EditBoxAccessor) this).litematicapass$setCursorPos(Mth.clamp(pos, 0, ((EditBox) (Object) this).getValue().length()));
			return;
		}
		this.litematicapass$moving = true;
		try {
			original.call(pos);
		} finally {
			this.litematicapass$moving = false;
		}
	}

	@Unique
	private boolean litematicapass$reading;

	@WrapMethod(method = "getCursorPosition", require = 0)
	private int litematicapass$readField(Operation<Integer> original) {
		if (this.litematicapass$reading) {
			return ((EditBoxAccessor) this).litematicapass$getCursorPos();
		}
		this.litematicapass$reading = true;
		try {
			return original.call();
		} finally {
			this.litematicapass$reading = false;
		}
	}
}
