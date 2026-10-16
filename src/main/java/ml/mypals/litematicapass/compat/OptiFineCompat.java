package ml.mypals.litematicapass.compat;

import ml.mypals.litematicapass.LitematicaPass;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * OptiFine (on Ornithe it is loaded by Legacy OptiFabric, mod id {@code optifabric}) takes the place Iris has on newer
 * versions: its shader pack renders the world into its own framebuffer ({@code dfb}) with its own programs, and only
 * its final pass writes Minecraft's framebuffer. OptiFine is not a build dependency, so its classes are reached by
 * reflection here and by name in the mixins under {@code mixin.optifine}.
 *
 * <p>Names used (OptiFine 1.12.2 HD U G5, {@code net.optifine.shaders.Shaders}): the public static fields
 * {@code shaderPackLoaded}, {@code isShadowPass}, {@code activeProgram}, {@code ProgramNone} and the public static
 * method {@code useProgram(Program)}.
 */
public final class OptiFineCompat {
	public static final boolean LOADED = FabricLoader.getInstance().isModLoaded("optifabric");

	private static boolean resolved;
	private static boolean usable;
	private static Field shaderPackLoaded;
	private static Field isShadowPass;
	private static Field activeProgram;
	private static Field programNone;
	private static Method useProgram;

	/** While true, ShadersMixin keeps OptiFine from binding its programs or changing the draw buffers. */
	private static boolean bypass;

	private OptiFineCompat() {}

	private static boolean resolve() {
		if (!resolved) {
			resolved = true;
			if (LOADED) {
				try {
					Class<?> shaders = Class.forName("net.optifine.shaders.Shaders");
					Class<?> program = Class.forName("net.optifine.shaders.Program");
					shaderPackLoaded = shaders.getField("shaderPackLoaded");
					isShadowPass = shaders.getField("isShadowPass");
					activeProgram = shaders.getField("activeProgram");
					programNone = shaders.getField("ProgramNone");
					useProgram = shaders.getMethod("useProgram", program);
					usable = true;
				} catch (ReflectiveOperationException | LinkageError e) {
					LitematicaPass.LOGGER.error("OptiFine's shader classes are not as expected; the schematic will not be routed", e);
				}
			}
		}
		return usable;
	}

	public static boolean isPackInUse() {
		return resolve() && getBoolean(shaderPackLoaded);
	}

	public static boolean isRenderingShadowPass() {
		return resolve() && getBoolean(isShadowPass);
	}

	public static boolean bypass() {
		return bypass;
	}

	/**
	 * Switches OptiFine to its "no program" state for a routed section and keeps it there, returning the program to
	 * give back to {@link #endBypass}. Call while OptiFine's own framebuffer is still bound: switching programs sets
	 * that framebuffer's draw buffers.
	 */
	public static Object beginBypass() {
		if (bypass || !resolve()) {
			return null;
		}
		try {
			Object saved = activeProgram.get(null);
			useProgram.invoke(null, programNone.get(null));
			bypass = true;
			return saved;
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Could not switch OptiFine's program", e);
		}
	}

	/** Lets OptiFine bind programs again and gives it back {@code saved}; call with its framebuffer bound again. */
	public static void endBypass(Object saved) {
		if (!bypass) {
			return;
		}
		bypass = false;
		if (saved != null) {
			try {
				useProgram.invoke(null, saved);
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException("Could not restore OptiFine's program", e);
			}
		}
	}

	private static boolean getBoolean(Field field) {
		try {
			return field.getBoolean(null);
		} catch (IllegalAccessException e) {
			return false;
		}
	}
}
