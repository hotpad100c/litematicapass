package ml.mypals.litematicapass.smoke;

import ml.mypals.litematicapass.compat.IrisCompat;
import ml.mypals.litematicapass.render.SchematicTarget;
import ml.mypals.litematicapass.smoke.mixin.CreateWorldScreenAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Development check, not part of the mod: deletes the previous test world, creates a new creative world, renders in
 * it for a while and quits. Run it with {@code ./gradlew :<version>:runSmokeClient}; the game exits normally when the
 * world was entered and rendered, and with code 2 when that did not happen in time (a crash exits on its own).
 */
public final class SmokeTest implements ClientModInitializer {
	private static final String TAG = "[litematicapass-smoke] ";
	private static final String WORLD = "LitematicaPass Smoke";
	/** Client ticks to stay in the world (20 per second). */
	private static final int TICKS_IN_WORLD = 400;
	/** Give up when the world has not been entered after this many client ticks. */
	private static final int TIMEOUT_TICKS = 20 * 60 * 5;

	private int ticks;
	private int ticksInWorld;
	private boolean requestedScreen;
	private boolean created;
	private boolean done;

	@Override
	public void onInitializeClient() {
		deleteOldWorld();
		ClientTickEvents.END_CLIENT_TICK.register(this::tick);
	}

	private void tick(Minecraft mc) {
		if (this.done) {
			return;
		}
		this.ticks++;
		if (mc.level != null && mc.player != null) {
			this.ticksInWorld++;
			if (this.ticksInWorld == 1) {
				log("in the world after " + this.ticks + " ticks");
			}
			if (this.ticksInWorld >= TICKS_IN_WORLD) {
				this.done = true;
				log("shader pack in use: " + IrisCompat.isPackInUse() + ", frames composited by LitematicaPass: "
					+ SchematicTarget.compositedFrames());
				log("RESULT: OK (entered the world and rendered " + TICKS_IN_WORLD + " ticks)");
				mc.stop();
			}
			return;
		}
		if (this.ticks > TIMEOUT_TICKS) {
			this.done = true;
			log("RESULT: FAILED (no world after " + TIMEOUT_TICKS + " ticks; screen: " + screen(mc) + ")");
			Runtime.getRuntime().halt(2);
		}
		if (this.created) {
			return;
		}
		if (screen(mc) instanceof CreateWorldScreen screen) {
			WorldCreationUiState state = screen.getUiState();
			state.setName(WORLD);
			state.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
			log("creating the world");
			this.created = true;
			((CreateWorldScreenAccessor) screen).litematicapass$onCreate();
			return;
		}
		// Wait for the title screen to settle (resource reload, onboarding), then open the create-world screen.
		if (!this.requestedScreen && overlay(mc) == null && this.ticks > 40) {
			this.requestedScreen = true;
			log("opening the create-world screen");
			//? if >=1.21.9 {
			CreateWorldScreen.openFresh(mc, () -> {});
			//?} else {
			/*CreateWorldScreen.openFresh(mc, null);
			*///?}
		}
	}

	//? if >=26.2 {
	private static Object screen(Minecraft mc) {
		return mc.gui.screen();
	}

	private static Object overlay(Minecraft mc) {
		return mc.gui.overlay();
	}
	//?} else {
	/*private static Object screen(Minecraft mc) {
		return mc.screen;
	}

	private static Object overlay(Minecraft mc) {
		return mc.getOverlay();
	}
	*///?}

	private static void deleteOldWorld() {
		Path saves = FabricLoader.getInstance().getGameDir().resolve("saves");
		if (!Files.isDirectory(saves)) {
			return;
		}
		try (Stream<Path> worlds = Files.list(saves)) {
			for (Path world : worlds.filter(p -> p.getFileName().toString().startsWith(WORLD)).toList()) {
				try (Stream<Path> files = Files.walk(world)) {
					for (Path file : files.sorted(Comparator.reverseOrder()).toList()) {
						Files.delete(file);
					}
				}
				log("deleted the old test world " + world.getFileName());
			}
		} catch (IOException e) {
			throw new IllegalStateException("Could not delete the old test world", e);
		}
	}

	private static void log(String message) {
		System.out.println(TAG + message);
		System.out.flush();
	}
}
