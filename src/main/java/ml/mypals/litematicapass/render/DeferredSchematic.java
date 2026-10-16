package ml.mypals.litematicapass.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureMap;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixes Litematica’s depth-ordering issues under OptiFine shaders by deferring schematic
 * rendering until after the full opaque scene is drawn, ensuring translucent blocks and
 * entities correctly occlude the schematic.
 */
public final class DeferredSchematic {
	private static final List<Runnable> PENDING = new ArrayList<>();
	private static boolean replaying;

	private DeferredSchematic() {}

	/** Holds {@code call} back until {@link #replay}, when the schematic is routed; returns whether it was held back. */
	public static boolean defer(Runnable call) {
		if (replaying || !SchematicTarget.isActive()) {
			return false;
		}
		PENDING.add(call);
		Stats.count("deferred schematic draws");
		return true;
	}

	/**
	 * Draws what was held back, with the state vanilla sets up around its block layers,
	 * and restores that state.
	 */
	public static void replay() {
		if (PENDING.isEmpty()) {
			return;
		}
		boolean alpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
		boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
		boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
		boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
		boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
		int shadeModel = GL11.glGetInteger(GL11.GL_SHADE_MODEL);
		int alphaFunc = GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC);
		float alphaRef = GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF);
		int texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

		replaying = true;
		try {
			Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
			GlStateManager.enableDepth();
			GlStateManager.depthMask(true);
			GlStateManager.disableBlend();
			GlStateManager.enableCull();
			GlStateManager.shadeModel(GL11.GL_SMOOTH);
			GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
			Stats.count("deferred schematic replays");
			for (Runnable call : PENDING) {
				call.run();
			}
		} finally {
			PENDING.clear();
			replaying = false;
			setEnabled(alpha, GL11.GL_ALPHA_TEST);
			setEnabled(blend, GL11.GL_BLEND);
			setEnabled(cull, GL11.GL_CULL_FACE);
			setEnabled(depthTest, GL11.GL_DEPTH_TEST);
			GlStateManager.depthMask(depthMask);
			GlStateManager.shadeModel(shadeModel);
			GlStateManager.alphaFunc(alphaFunc, alphaRef);
			GlStateManager.bindTexture(texture);
		}
	}

	/** The state each layer expects: solid without alpha test, cutout with it, and cutout without mipmap blur. */
	public static void prepareLayer(boolean alphaTest, boolean noMipmapBlur) {
		if (alphaTest) {
			GlStateManager.enableAlpha();
		} else {
			GlStateManager.disableAlpha();
		}
		ITextureObject blocks = Minecraft.getMinecraft().getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		if (noMipmapBlur) {
			blocks.setBlurMipmap(false, false);
		}
	}

	public static void finishLayer(boolean noMipmapBlur) {
		if (noMipmapBlur) {
			Minecraft.getMinecraft().getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();
		}
	}

	public static boolean isReplaying() {
		return replaying;
	}

	static void endFrame() {
		if (!PENDING.isEmpty()) {
			Stats.count("deferred schematic draws dropped");
			PENDING.clear();
		}
	}

	private static void setEnabled(boolean enabled, int capability) {
		if (capability == GL11.GL_ALPHA_TEST) {
			if (enabled) {
				GlStateManager.enableAlpha();
			} else {
				GlStateManager.disableAlpha();
			}
		} else if (capability == GL11.GL_BLEND) {
			if (enabled) {
				GlStateManager.enableBlend();
			} else {
				GlStateManager.disableBlend();
			}
		} else if (capability == GL11.GL_CULL_FACE) {
			if (enabled) {
				GlStateManager.enableCull();
			} else {
				GlStateManager.disableCull();
			}
		} else if (capability == GL11.GL_DEPTH_TEST) {
			if (enabled) {
				GlStateManager.enableDepth();
			} else {
				GlStateManager.disableDepth();
			}
		}
	}
}
