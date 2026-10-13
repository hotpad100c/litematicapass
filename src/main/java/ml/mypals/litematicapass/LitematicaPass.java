package ml.mypals.litematicapass;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import ml.mypals.litematicapass.compat.IrisCompat;
import ml.mypals.litematicapass.render.DebugView;
import ml.mypals.litematicapass.render.SchematicTarget;
import net.fabricmc.api.ClientModInitializer;
//? if >=1.19 {
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
//?} else {
/*import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.network.chat.TextComponent;
*///?}
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
//? if >=1.18 {
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//?} else {
/*import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
*///?}

public class LitematicaPass implements ClientModInitializer {
	public static final String MOD_ID = "litematicapass";

	//? if >=1.18 {
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	//?} else {
	/*// Minecraft logs through Log4j directly before 1.18.
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
	*///?}

	@Override
	public void onInitializeClient() {
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> SchematicTarget.close());
		//? if >=1.19 {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(buildCommand()));
		//?} else {
		/*ClientCommandManager.DISPATCHER.register(buildCommand());
		*///?}
	}

	/** {@code /litematicapass debug [view]}: show the buffers of the schematic pipeline instead of the frame. */
	private static LiteralArgumentBuilder<FabricClientCommandSource> buildCommand() {
		LiteralArgumentBuilder<FabricClientCommandSource> debug = ClientCommandManager.literal("debug")
			.executes(context -> {
				context.getSource().sendFeedback(text("litematicapass debug view: " + DebugView.mode().id()));
				for (DebugView.Mode mode : DebugView.Mode.values()) {
					context.getSource().sendFeedback(text("  " + mode.id() + " - " + mode.description));
				}
				return 1;
			});

		for (DebugView.Mode mode : DebugView.Mode.values()) {
			debug.then(ClientCommandManager.literal(mode.id()).executes(context -> {
				DebugView.setMode(mode);
				context.getSource().sendFeedback(text("litematicapass debug view: " + mode.id() + " (" + mode.description + ")"));
				if (mode != DebugView.Mode.OFF && !IrisCompat.isPackInUse()) {
					context.getSource().sendFeedback(text("No shader pack is in use: the schematic is not routed, so the view will not show."));
				}
				return 1;
			}));
		}

		return ClientCommandManager.literal(MOD_ID).then(debug);
	}

	private static Component text(String text) {
		//? if >=1.19 {
		return Component.literal(text);
		//?} else {
		/*return new TextComponent(text);
		*///?}
	}

	public static ResourceLocation id(String path) {
		//? if >=1.21 {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
		//?} else {
		/*return new ResourceLocation(MOD_ID, path);
		*///?}
	}
}
