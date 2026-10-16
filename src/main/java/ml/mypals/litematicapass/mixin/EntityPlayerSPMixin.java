package ml.mypals.litematicapass.mixin;

import ml.mypals.litematicapass.LitematicaPass;
import net.minecraft.client.entity.EntityPlayerSP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** {@code /litematicapass ...} is handled on the client and never sent to the server (see LitematicaPass.handleCommand). */
@Mixin(EntityPlayerSP.class)
public class EntityPlayerSPMixin {
	@Inject(method = "sendChatMessage(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
	private void litematicapass$clientCommand(String message, CallbackInfo ci) {
		if (LitematicaPass.handleCommand(message)) {
			ci.cancel();
		}
	}
}
