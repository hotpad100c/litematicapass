package ml.mypals.litematicapass.smoke;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import ml.mypals.litematicapass.compat.IrisCompat;
import ml.mypals.litematicapass.render.DebugView;
import ml.mypals.litematicapass.render.SchematicTarget;
import ml.mypals.litematicapass.smoke.mixin.CreateWorldScreenAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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
	private static final String VERSION = FabricLoader.getInstance().getModContainer("minecraft").orElseThrow()
		.getMetadata().getVersion().getFriendlyString();
	/** Client ticks to stay in the world (20 per second). */
	private static final int TICKS_IN_WORLD = 400;
	/** Give up when the world has not been entered after this many client ticks. */
	private static final int TIMEOUT_TICKS = 20 * 60 * 5;

	private int ticks;
	private int ticksInWorld;
	private int placedAt = -1;
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
		if (this.ticks == 1) {
			// The check runs unattended; a pause menu would cover the screenshots when the window loses focus.
			mc.options.pauseOnLostFocus = false;
		}
		if (mc.level != null && mc.player != null) {
			this.ticksInWorld++;
			if (this.ticksInWorld == 1) {
				log("in the world after " + this.ticks + " ticks");
			}
			this.showcase(mc);
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

	/**
	 * Lifts the player into open sky facing south, places a small schematic wall in front of it and takes screenshots
	 * of the normal view and some debug views (run/screenshots/litematicapass-smoke-*.png).
	 */
	private void showcase(Minecraft mc) {
		if (this.ticksInWorld == 20) {
			mc.player.connection.sendCommand("tp @s ~ 200 ~ 0 10");
		}
		if (this.placedAt < 0 && this.ticksInWorld > 20 && mc.player.getY() > 190) {
			// Hover in place (creative flight) so the view stays on the schematic.
			mc.player.getAbilities().flying = true;
			mc.player.onUpdateAbilities();
			placeSchematic(mc);
			this.placedAt = this.ticksInWorld;
		}
		if (this.placedAt < 0) {
			return;
		}
		switch (this.ticksInWorld - this.placedAt) {
			case 100 -> screenshot(mc, "normal");
			case 101 -> DebugView.setMode(DebugView.Mode.COLOR);
			case 115 -> screenshot(mc, "color");
			case 116 -> DebugView.setMode(DebugView.Mode.SCHEMATIC_DEPTH);
			case 130 -> screenshot(mc, "schematic_depth");
			case 131 -> DebugView.setMode(DebugView.Mode.SCENE_DEPTH);
			case 145 -> screenshot(mc, "scene_depth");
			case 146 -> DebugView.setMode(DebugView.Mode.OFF);
			default -> {
			}
		}
	}

	/**
	 * A 21 x 12 wall, 4 blocks in front of the player, filling the view (the first-person arm included, which must stay
	 * in front of it): rows of stone, glass, gold and planks. In front of it, at eye level, a row of chests (block
	 * entities, drawn by their renderer instead of the chunk mesh), which the wall behind them must not hide.
	 */
	private static void placeSchematic(Minecraft mc) {
		AreaSelection area = new AreaSelection();
		area.addSubRegionBox(new Box(BlockPos.ZERO, new BlockPos(20, 11, 1), "wall"), false);
		LitematicaSchematic schematic = LitematicaSchematic.createEmptySchematic(area, "litematicapass");
		LitematicaBlockStateContainer container = schematic.getSubRegionContainer("wall");
		BlockState[] rows = { Blocks.STONE.defaultBlockState(), Blocks.GLASS.defaultBlockState(), Blocks.GOLD_BLOCK.defaultBlockState(),
			Blocks.OAK_PLANKS.defaultBlockState() };
		for (int y = 0; y < 12; y++) {
			for (int x = 0; x < 21; x++) {
				container.set(x, y, 1, rows[y % 4]);
			}
		}
		for (int x = 0; x < 21; x++) {
			container.set(x, 5, 0, Blocks.CHEST.defaultBlockState());
		}
		BlockPos origin = mc.player.blockPosition().offset(-10, -6, 3);
		SchematicPlacement placement = SchematicPlacement.createFor(schematic, origin, "litematicapass smoke", true, true);
		DataManager.getSchematicPlacementManager().addSchematicPlacement(placement, false);
		log("placed the test schematic at " + origin);
	}

	private static void screenshot(Minecraft mc, String view) {
		String name = "litematicapass-smoke-" + VERSION + "-" + view + ".png";
		//? if >=26.2 {
		Screenshot.grab(mc.gameDirectory, name, mc.gameRenderer.mainRenderTarget(), 1, message -> log("screenshot " + name));
		//?} else if >=1.21.6 {
		/*Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), 1, message -> log("screenshot " + name));
		*///?} else {
		/*Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), message -> log("screenshot " + name));
		*///?}
	}

	private static void deleteOldWorld() {
		// Litematica keeps the placements of a world by its name; start without the ones of earlier runs.
		Path litematica = FabricLoader.getInstance().getConfigDir().resolve("litematica");
		if (Files.isDirectory(litematica)) {
			try (Stream<Path> files = Files.list(litematica)) {
				for (Path file : files.filter(p -> p.getFileName().toString().startsWith("litematica_" + WORLD)).toList()) {
					Files.delete(file);
				}
			} catch (IOException e) {
				throw new IllegalStateException("Could not delete Litematica's data of the old test world", e);
			}
		}
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
