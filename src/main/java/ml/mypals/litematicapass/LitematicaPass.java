package ml.mypals.litematicapass;

import ml.mypals.litematicapass.compat.OptiFineCompat;
import ml.mypals.litematicapass.render.DebugView;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;

public final class LitematicaPass {
	public static final String MOD_ID = "litematicapass";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

	private static final String COMMAND = "/" + MOD_ID;

	private LitematicaPass() {}

	/**
	 * {@code /litematicapass debug [view]}: show the buffers of the schematic pipeline instead of the frame. 1.12 has no
	 * client command API (Ornithe's standard libraries have none either), so the chat message is intercepted
	 * (EntityPlayerSPMixin); returns whether it was this command.
	 */
	public static boolean handleCommand(String message) {
		String[] words = message.trim().split("\\s+");
		if (!words[0].equalsIgnoreCase(COMMAND)) {
			return false;
		}
		if (words.length < 2 || !words[1].equalsIgnoreCase("debug")) {
			feedback("Usage: " + COMMAND + " debug [view]");
			return true;
		}
		if (words.length == 2) {
			feedback("litematicapass debug view: " + DebugView.mode().id());
			for (DebugView.Mode mode : DebugView.Mode.values()) {
				feedback("  " + mode.id() + " - " + mode.description);
			}
			return true;
		}
		String id = words[2].toLowerCase(Locale.ROOT);
		for (DebugView.Mode mode : DebugView.Mode.values()) {
			if (mode.id().equals(id)) {
				DebugView.setMode(mode);
				feedback("litematicapass debug view: " + mode.id() + " (" + mode.description + ")");
				if (mode != DebugView.Mode.OFF && !OptiFineCompat.isPackInUse()) {
					feedback("No shader pack is in use: the schematic is not routed, so the view will not show.");
				}
				return true;
			}
		}
		feedback("Unknown debug view: " + id);
		return true;
	}

	private static void feedback(String text) {
		Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentString(text));
	}
}
