package ml.mypals.litematicapass;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import ml.mypals.litematicapass.compat.IrisCompat;
import ml.mypals.litematicapass.render.DebugView;
import ml.mypals.litematicapass.render.SchematicTarget;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LitematicaPass implements ClientModInitializer {
	public static final String MOD_ID = "litematicapass";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> SchematicTarget.close());
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(buildCommand()));
	}

	/** {@code /litematicapass debug [view]}: show the buffers of the schematic pipeline instead of the frame. */
	private static LiteralArgumentBuilder<FabricClientCommandSource> buildCommand() {
		LiteralArgumentBuilder<FabricClientCommandSource> debug = ClientCommands.literal("debug")
			.executes(context -> {
				context.getSource().sendFeedback(Component.literal("litematicapass debug view: " + DebugView.mode().id()));
				for (DebugView.Mode mode : DebugView.Mode.values()) {
					context.getSource().sendFeedback(Component.literal("  " + mode.id() + " - " + mode.description));
				}
				return 1;
			});

		for (DebugView.Mode mode : DebugView.Mode.values()) {
			debug.then(ClientCommands.literal(mode.id()).executes(context -> {
				DebugView.setMode(mode);
				context.getSource().sendFeedback(Component.literal("litematicapass debug view: " + mode.id() + " (" + mode.description + ")"));
				if (mode != DebugView.Mode.OFF && !IrisCompat.isPackInUse()) {
					context.getSource().sendFeedback(Component.literal("No shader pack is in use: the schematic is not routed, so the view will not show."));
				}
				return 1;
			}));
		}

		return ClientCommands.literal(MOD_ID).then(debug);
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
