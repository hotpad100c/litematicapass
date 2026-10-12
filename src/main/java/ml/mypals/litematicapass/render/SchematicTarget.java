package ml.mypals.litematicapass.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import ml.mypals.litematicapass.compat.IrisCompat;

/**
 * 一个独立的 颜色+深度 渲染目标，在激活光影包时，schematic 的绘制会被重定向到这里。
 * 其深度从主深度初始化，因此世界仍然会遮挡 schematic；
 * 在帧结束时，渲染结果会被深度合并回主目标上（ {@link SchematicComposite}）。
 */
public final class SchematicTarget {
	private static TextureTarget target;

	private static int bypassDepth;
	private static boolean bypassApplied;
	private static boolean bypassSaved;
	private static boolean skipExtensionSaved;

	private static boolean preparedThisFrame;
	private static boolean usedThisFrame;

	private SchematicTarget() {}

	public static boolean isActive() {
		return IrisCompat.isPackInUse() && !IrisCompat.isRenderingShadowPass();
	}

	public static void begin() {
		if (bypassDepth++ == 0) {
			bypassApplied = isActive();
			if (bypassApplied) {
				bypassSaved = IrisCompat.bypass();
				IrisCompat.setBypass(true);
				// Litematica's chunk meshes are built in the plain BLOCK format (see ChunkRendererSchematicVboMixin);
				// make sure Iris does not widen the vertex format the pipeline is bound with either.
				skipExtensionSaved = IrisCompat.skipExtension();
				IrisCompat.setSkipExtension(true);
			}
		}
	}

	public static void end() {
		if (bypassDepth > 0 && --bypassDepth == 0 && bypassApplied) {
			IrisCompat.setBypass(bypassSaved);
			IrisCompat.setSkipExtension(skipExtensionSaved);
			bypassApplied = false;
		}
	}

	public static boolean isRouting() {
		return bypassDepth > 0 && bypassApplied;
	}

	public static RenderTarget targetFor(RenderTarget main) {
		return isRouting() ? prepareForDraw() : main;
	}

	public static RenderTarget prepareForDraw() {
		ensureTarget();
		if (!preparedThisFrame) {
			RenderTarget main = Gpu.mainTarget();
			Gpu.clear(target);
			DepthSeed.seed(main, target);
			preparedThisFrame = true;
		}
		usedThisFrame = true;
		return target;
	}

	private static void ensureTarget() {
		RenderTarget main = Gpu.mainTarget();
		int width = Math.max(1, main.width);
		int height = Math.max(1, main.height);
		if (target == null) {
			target = Gpu.colorDepthTarget("litematicapass_schematic", width, height);
		} else if (target.width != width || target.height != height) {
			target.resize(width, height);
		}
	}

	/** Merge the independent target onto the main one and reset the per-frame state. */
	public static void compositeAndReset() {
		try {
			if (DebugView.mode() != DebugView.Mode.OFF && isActive()) {
				showDebugView();
			} else if (target != null && usedThisFrame) {
				RenderTarget main = Gpu.mainTarget();
				boolean saved = IrisCompat.bypass();
				IrisCompat.setBypass(true);
				try {
					SchematicComposite.draw(main, target);
				} finally {
					IrisCompat.setBypass(saved);
				}
			}
		} finally {
			preparedThisFrame = false;
			usedThisFrame = false;
			SceneDepth.endFrame();
			SchematicEntities.endFrame();
		}
	}

	/** Replace the frame with one of the debug views; the independent target is cleared first if nothing used it. */
	private static void showDebugView() {
		RenderTarget main = Gpu.mainTarget();
		ensureTarget();
		if (!usedThisFrame) {
			Gpu.clear(target);
		}
		boolean saved = IrisCompat.bypass();
		IrisCompat.setBypass(true);
		try {
			DebugView.draw(main, target);
		} finally {
			IrisCompat.setBypass(saved);
		}
	}

	public static void close() {
		if (target != null) {
			target.destroyBuffers();
			target = null;
		}
		preparedThisFrame = false;
		usedThisFrame = false;
		SceneDepth.close();
		SchematicEntities.close();
	}
}
