package ml.mypals.litematicapass.render;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Method;

public final class SceneSize {
	private static final boolean SUPER_RESOLUTION_LOADED =
			FabricLoader.getInstance().isModLoaded("super_resolution");

	private static Method srRenderWidth;
	private static Method srRenderHeight;
	private static boolean srLooked;

	public static boolean mergeStretched() {
		return width() < Math.max(1, Minecraft.getInstance().getWindow().getWidth())
			|| height() < Math.max(1, Minecraft.getInstance().getWindow().getHeight());
	}

	public static int width() {
		int window = Math.max(1, Minecraft.getInstance().getWindow().getWidth());
		Integer render = superResolutionSize(true);
		return render == null ? window : render;
	}

	public static int height() {
		int window = Math.max(1, Minecraft.getInstance().getWindow().getHeight());
		Integer render = superResolutionSize(false);
		return render == null ? window : render;
	}

	private static Integer superResolutionSize(boolean width) {
		lookUpSuperResolution();
		if (srRenderWidth == null) {
			return null;
		}
		try {
			return (Integer) (width ? srRenderWidth : srRenderHeight).invoke(null);
		} catch (ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}

	private static void lookUpSuperResolution() {
		if (srLooked || !SUPER_RESOLUTION_LOADED) {
			return;
		}
		srLooked = true;
		for (String className : new String[] {
			"io.homo.superresolution.api.SuperResolutionAPI",
			"io.homo.superresolution.common.minecraft.handler.RenderHandlerManager"
		}) {
			try {
				Class<?> type = Class.forName(className);
				srRenderWidth = type.getMethod("getRenderWidth");
				srRenderHeight = type.getMethod("getRenderHeight");
				return;
			} catch (ReflectiveOperationException | LinkageError e) {
				srRenderWidth = null;
				srRenderHeight = null;
			}
		}
	}
}
