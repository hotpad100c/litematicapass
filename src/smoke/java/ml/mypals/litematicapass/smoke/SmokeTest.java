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
//? if >=1.19.4 {
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
//?} else if >=1.17 {
/*import net.minecraft.client.gui.components.CycleButton;
*///?} else {
/*import net.minecraft.client.gui.components.Button;
*///?}

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
			//? if >=1.19.4 {
			WorldCreationUiState state = screen.getUiState();
			state.setName(WORLD);
			state.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
			//?} else if >=1.17 {
			/*CreateWorldScreenAccessor legacy = (CreateWorldScreenAccessor) screen;
			legacy.litematicapass$nameEdit().setValue(WORLD);
			CycleButton<?> mode = legacy.litematicapass$modeButton();
			for (int i = 0; i < 4 && !"CREATIVE".equals(String.valueOf(mode.getValue())); i++) {
				mode.onPress();
			}
			*///?} else {
			/*CreateWorldScreenAccessor legacy = (CreateWorldScreenAccessor) screen;
			legacy.litematicapass$nameEdit().setValue(WORLD);
			Button mode = legacy.litematicapass$modeButton();
			for (int i = 0; i < 4 && !legacy.litematicapass$commands(); i++) {
				mode.onPress();
			}
			*///?}
			log("creating the world");
			this.created = true;
			((CreateWorldScreenAccessor) screen).litematicapass$onCreate();
			return;
		}
		// Wait for the title screen to settle (resource reload, onboarding), then open the create-world screen.
		if (!this.requestedScreen && overlay(mc) == null && this.ticks > 40) {
			this.requestedScreen = true;
			log("opening the create-world screen");
			//? if >=1.19 {
			CreateWorldScreen.openFresh(mc, null);
			//?} else if >=1.18.2 {
			/*mc.setScreen(CreateWorldScreen.createFresh(null));
			*///?} else {
			/*mc.setScreen(CreateWorldScreen.create(null));
			*///?}
		}
	}

	private static Object screen(Minecraft mc) {
		return mc.screen;
	}

	private static Object overlay(Minecraft mc) {
		return mc.getOverlay();
	}

	/**
	 * Lifts the player into open sky facing south, places a small schematic wall in front of it and takes screenshots
	 * of the normal view and some debug views (run/screenshots/litematicapass-smoke-*.png).
	 */
	private void showcase(Minecraft mc) {
		if (this.ticksInWorld == 20) {
			//? if >=1.19.4 {
			mc.player.connection.sendCommand("tp @s ~ 200 ~ 0 10");
			//?} else {
			/*// The command API changes in every version before; the singleplayer owner may simply move there.
			mc.player.absMoveTo(mc.player.getX(), 200, mc.player.getZ(), 0, 10);
			*///?}
		}
		if (this.placedAt < 0 && this.ticksInWorld > 20 && mc.player.getY() > 190) {
			// Hover in place (creative flight) so the view stays on the schematic.
			//? if >=1.17 {
			mc.player.getAbilities().flying = true;
			//?} else {
			/*mc.player.abilities.flying = true;
			*///?}
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
	 * A 21 x 12 wall, 3 blocks in front of the player, filling the view (the first-person arm included, which must stay
	 * in front of it): rows of stone, glass, gold and planks.
	 */
	private static void placeSchematic(Minecraft mc) {
		AreaSelection area = new AreaSelection();
		area.addSubRegionBox(new Box(BlockPos.ZERO, new BlockPos(20, 11, 0), "wall"), false);
		LitematicaSchematic schematic = LitematicaSchematic.createEmptySchematic(area, "litematicapass");
		LitematicaBlockStateContainer container = schematic.getSubRegionContainer("wall");
		BlockState[] rows = { Blocks.STONE.defaultBlockState(), Blocks.GLASS.defaultBlockState(), Blocks.GOLD_BLOCK.defaultBlockState(),
			Blocks.OAK_PLANKS.defaultBlockState() };
		for (int y = 0; y < 12; y++) {
			for (int x = 0; x < 21; x++) {
				container.set(x, y, 0, rows[y % 4]);
			}
		}
		BlockPos origin = mc.player.blockPosition().offset(-10, -6, 3);
		SchematicPlacement placement = SchematicPlacement.createFor(schematic, origin, "litematicapass smoke", true, true);
		DataManager.getSchematicPlacementManager().addSchematicPlacement(placement, false);
		log("placed the test schematic at " + origin);
	}

	private static void screenshot(Minecraft mc, String view) {
		String name = "litematicapass-smoke-" + VERSION + "-" + view + ".png";
		//? if >=1.18 {
		Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), message -> log("screenshot " + name));
		//?} else {
		/*// 1.17 also takes the size; the 1.17.1 build runs on both, so look for whichever one there is.
		java.util.function.Consumer<net.minecraft.network.chat.Component> done = message -> log("screenshot " + name);
		com.mojang.blaze3d.pipeline.RenderTarget target = mc.getMainRenderTarget();
		try {
			try {
				Screenshot.class.getMethod("grab", java.io.File.class, String.class, com.mojang.blaze3d.pipeline.RenderTarget.class,
					java.util.function.Consumer.class).invoke(null, mc.gameDirectory, name, target, done);
			} catch (NoSuchMethodException e) {
				Screenshot.class.getMethod("grab", java.io.File.class, String.class, int.class, int.class, com.mojang.blaze3d.pipeline.RenderTarget.class,
					java.util.function.Consumer.class).invoke(null, mc.gameDirectory, name, target.width, target.height, target, done);
			}
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Could not take the screenshot " + name, e);
		}
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
