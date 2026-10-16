package ml.mypals.litematicapass.render;

import ml.mypals.litematicapass.LitematicaPass;
import ml.mypals.litematicapass.compat.OptiFineCompat;

/**
 * 一个独立的 颜色+深度 渲染目标，在激活光影包（OptiFine）时，schematic 的绘制会被重定向到这里。
 * 其深度从光影包的场景深度初始化，因此世界仍然会遮挡 schematic；
 * 在 OptiFine 的合成（composite/final）结束后，渲染结果会被深度合并到 Minecraft 的帧缓冲上（{@link SchematicComposite}）。
 * 尺寸跟随 OptiFine 的场景深度（它按自己的渲染分辨率绘制世界）。
 */
public final class SchematicTarget {
	private static final Gpu.Target TARGET = new Gpu.Target();

	private static int bypassDepth;
	private static boolean bypassApplied;
	private static Object savedProgram;

	private static boolean preparedThisFrame;
	private static boolean usedThisFrame;
	private static long compositedFrames;
	private static boolean warnedNoDepth;
	private static final Runnable NOTHING = () -> {};
	private static Runnable restoreOutput = NOTHING;

	private SchematicTarget() {}

	/** How many frames merged the independent target onto the main one (for the debug command and checks). */
	public static long compositedFrames() {
		return compositedFrames;
	}

	public static boolean isActive() {
		return OptiFineCompat.isPackInUse() && !OptiFineCompat.isRenderingShadowPass();
	}

	public static void begin() {
		if (bypassDepth++ == 0) {
			bypassApplied = isActive();
			Stats.count(bypassApplied ? "routed sections" : "unrouted sections (no shader pack or shadow pass)");
			if (bypassApplied) {
				// Prepare while OptiFine's framebuffer is still bound: the seed reads its depth, and switching
				// OptiFine's program sets that framebuffer's draw buffers.
				Gpu.Target target = prepareForDraw();
				savedProgram = OptiFineCompat.beginBypass();
				// Minecraft 1.12 draws into whatever framebuffer is bound; bind ours for the routed section.
				restoreOutput = Gpu.bindForDrawing(target);
			}
		}
	}

	public static void end() {
		if (bypassDepth > 0 && --bypassDepth == 0 && bypassApplied) {
			restoreOutput.run();
			restoreOutput = NOTHING;
			OptiFineCompat.endBypass(savedProgram);
			savedProgram = null;
			bypassApplied = false;
		}
	}

	public static boolean isRouting() {
		return bypassDepth > 0 && bypassApplied;
	}

	private static Gpu.Target prepareForDraw() {
		if (!preparedThisFrame) {
			Gpu.Texture scene = SceneDepth.live();
			if (scene != null) {
				Gpu.ensureTarget(TARGET, scene.width, scene.height);
			} else {
				Gpu.Output main = Gpu.mainOutput();
				Gpu.ensureTarget(TARGET, main != null ? main.width : 1, main != null ? main.height : 1);
			}
			Gpu.clear(TARGET);
			if (scene != null) {
				DepthSeed.seed(TARGET);
			} else if (!warnedNoDepth) {
				warnedNoDepth = true;
				LitematicaPass.LOGGER.warn("No scene depth to seed the schematic target from; the schematic will show through the scene");
			}
			preparedThisFrame = true;
		}
		usedThisFrame = true;
		return TARGET;
	}

	/** Merge the independent target onto Minecraft's framebuffer and reset the per-frame state. */
	public static void compositeAndReset() {
		try {
			Stats.count("merge calls");
			Gpu.Output main = Gpu.mainOutput();
			if (main == null) {
				Stats.count("merge skipped: Minecraft has no framebuffer");
				return;
			}
			if (DebugView.mode() != DebugView.Mode.OFF && isActive()) {
				if (!usedThisFrame) {
					Gpu.Texture scene = SceneDepth.live();
					Gpu.ensureTarget(TARGET, scene != null ? scene.width : main.width, scene != null ? scene.height : main.height);
					Gpu.clear(TARGET);
				}
				DebugView.draw(main, TARGET);
				Stats.count("debug view drawn");
			} else if (TARGET.framebuffer != 0 && usedThisFrame) {
				SchematicComposite.draw(main, TARGET);
				compositedFrames++;
				Stats.count("merged");
			} else if (DebugView.mode() != DebugView.Mode.OFF) {
				Stats.count("debug view skipped: not active (no shader pack or shadow pass)");
			}
		} finally {
			preparedThisFrame = false;
			usedThisFrame = false;
			SceneDepth.endFrame();
		}
	}
}
