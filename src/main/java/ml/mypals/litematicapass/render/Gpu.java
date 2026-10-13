package ml.mypals.litematicapass.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
//? if >=1.17 {
import com.mojang.blaze3d.pipeline.TextureTarget;
//?}
import ml.mypals.litematicapass.LitematicaPass;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import ml.mypals.litematicapass.mixin.MinecraftAccessor;

/**
 * The GPU work of the mod, behind one small API so the render classes share one body: render targets, and
 * full-screen passes ({@link #colorPipeline} / {@link #depthPipeline} drawn with {@link #draw}). From 1.21.5 this
 * uses Minecraft's RenderPipeline / RenderPass API; before, it is plain OpenGL.
 */
final class Gpu {
	/**
	 * The depth a cleared depth buffer holds. 26.2 switched to reversed Z (near = 1, far = 0); older versions use
	 * near = 0, far = 1. The shaders see {@code STANDARD_DEPTH} on the older versions and compare "closeness".
	 */
	static final double FAR_DEPTH = 1.0;

	private Gpu() {}

	static RenderTarget mainTarget() {
		// Before 1.21.6 routing redirects getMainRenderTarget() (see MinecraftMixin); read the field itself.
		return ((MinecraftAccessor) Minecraft.getInstance()).litematicapass$mainRenderTarget();
	}

	/** A colour + depth target. */
	static RenderTarget colorDepthTarget(String label, int width, int height) {
		//? if >=1.17 {
		RenderTarget target = new TextureTarget(width, height, true, Minecraft.ON_OSX);
		//?} else {
		/*// Before 1.17 RenderTarget is the texture-backed target itself.
		RenderTarget target = new RenderTarget(width, height, true, Minecraft.ON_OSX);
		*///?}
		target.setClearColor(0, 0, 0, 0);
		return target;
	}

	static void resize(RenderTarget target, int width, int height) {
		target.resize(width, height, Minecraft.ON_OSX);
	}

	/** A target whose depth is used; older versions always give it a colour texture too. */
	static RenderTarget depthTarget(String label, int width, int height) {
		return colorDepthTarget(label, width, height);
	}

	/** Clears the colour to transparent black and the depth to {@link #FAR_DEPTH}. */
	static void clear(RenderTarget target) {
		// RenderTarget.clear() leaves the default framebuffer bound; keep whatever was bound.
		int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
		int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
		int[] viewport = new int[4];
		GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
		clearUnbound(target);
		GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
		GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
		GlStateManager._viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
	}

	private static void clearUnbound(RenderTarget target) {
		target.clear(Minecraft.ON_OSX);
	}

	private static int depthTextureReadFramebuffer;

	/** Copies the depth texture {@code texture}, of {@code target}'s size, into the depth of {@code target}. */
	static void copyDepthTexture(int texture, RenderTarget target) {
		int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
		int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
		if (depthTextureReadFramebuffer == 0) {
			depthTextureReadFramebuffer = GL30.glGenFramebuffers();
		}
		GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, depthTextureReadFramebuffer);
		GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, texture, 0);
		GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, target.frameBufferId);
		GL30.glBlitFramebuffer(0, 0, target.width, target.height, 0, 0, target.width, target.height, GL11.GL_DEPTH_BUFFER_BIT,
			GL11.GL_NEAREST);
		GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
		GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
	}

	/** Copies the depth of {@code source} into {@code target}. */
	static void copyDepth(RenderTarget target, RenderTarget source) {
		// copyDepthFrom leaves the default framebuffer bound; this runs inside the shader pack's passes.
		int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
		int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
		target.copyDepthFrom(source);
		GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
		GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
	}

	/**
	 * Points the output of RenderType draws at {@code target}; run the returned action to undo it. 26.3 draws features
	 * into a pass we open ourselves, and before 1.21.6 there is no such override, but while routing the schematic target
	 * is handed out as the main target (MinecraftMixin).
	 */
	static Runnable redirectOutput(RenderTarget target) {
		return () -> {};
	}

	/**
	 * Makes {@code target} the framebuffer that following draws go to; run the returned action to bind back what was
	 * bound. Only before 1.21.5: there draws go to whatever framebuffer is bound (on 1.21.1 render types do not even
	 * bind the main target), while later versions name the target of every pass.
	 */
	static Runnable bindForDrawing(RenderTarget target) {
		int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
		int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
		int[] viewport = new int[4];
		GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
		GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, target.frameBufferId);
		GlStateManager._viewport(0, 0, target.width, target.height);
		return () -> {
			GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
			GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
			GlStateManager._viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
		};
	}

	/** A texture a full-screen pass samples: the colour or the depth of a target, read with {@code texelFetch}. */
	record Sampler(String name, RenderTarget target, boolean depth) {}

	static Sampler color(String name, RenderTarget target) {
		return new Sampler(name, target, false);
	}

	static Sampler depth(String name, RenderTarget target) {
		return new Sampler(name, target, true);
	}

	/**
	 * A full-screen pass writing RGB (not alpha) into a target's colour, without depth; {@code premultiplied} blends
	 * premultiplied colour over it, otherwise it replaces it.
	 */
	static Pipeline colorPipeline(String location, String fragmentShader, boolean premultiplied, Map<String, Integer> defines,
		String... samplers) {
		return new Pipeline(location, fragmentShader, false, premultiplied, defines, samplers);
	}

	/** A full-screen pass writing only a target's depth, with the default depth test. */
	static Pipeline depthPipeline(String location, String fragmentShader, String... samplers) {
		return new Pipeline(location, fragmentShader, true, false, Map.of(), samplers);
	}

	// Before 1.21.5 there is no RenderPipeline / RenderPass API: compile the shaders and draw with OpenGL.
	static final class Pipeline {
		private static final String VERTEX_SHADER = """
			#version 330
			void main() {
			    vec2 corner = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
			    gl_Position = vec4(corner * 2.0 - 1.0, 0.0, 1.0);
			}
			""";

		private final String fragmentShader;
		private final boolean depthOnly;
		private final boolean premultiplied;
		private final Map<String, Integer> defines;
		private final String[] samplers;
		private int program;
		private int[] samplerLocations;
		private boolean failed;

		private Pipeline(String location, String fragmentShader, boolean depthOnly, boolean premultiplied, Map<String, Integer> defines,
			String[] samplers) {
			this.fragmentShader = fragmentShader;
			this.depthOnly = depthOnly;
			this.premultiplied = premultiplied;
			this.defines = defines;
			this.samplers = samplers;
		}

		private void compile() {
			String source = readShader("/assets/" + LitematicaPass.MOD_ID + "/shaders/" + this.fragmentShader + ".fsh");
			StringBuilder header = new StringBuilder("#define STANDARD_DEPTH\n");
			this.defines.forEach((key, value) -> header.append("#define ").append(key).append(' ').append(value).append('\n'));
			int afterVersion = source.indexOf('\n') + 1;
			source = source.substring(0, afterVersion) + header + source.substring(afterVersion);

			int vertex = compileShader(GL20.GL_VERTEX_SHADER, VERTEX_SHADER);
			int fragment = compileShader(GL20.GL_FRAGMENT_SHADER, source);
			int program = GL20.glCreateProgram();
			GL20.glAttachShader(program, vertex);
			GL20.glAttachShader(program, fragment);
			GL20.glLinkProgram(program);
			GL20.glDeleteShader(vertex);
			GL20.glDeleteShader(fragment);
			if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
				throw new IllegalStateException("Could not link " + this.fragmentShader + ": " + GL20.glGetProgramInfoLog(program));
			}
			this.samplerLocations = new int[this.samplers.length];
			for (int i = 0; i < this.samplers.length; i++) {
				this.samplerLocations[i] = GL20.glGetUniformLocation(program, this.samplers[i]);
			}
			this.program = program;
		}

		private int location(String sampler) {
			for (int i = 0; i < this.samplers.length; i++) {
				if (this.samplers[i].equals(sampler)) {
					return this.samplerLocations[i];
				}
			}
			return -1;
		}

		private static int compileShader(int type, String source) {
			int shader = GL20.glCreateShader(type);
			GL20.glShaderSource(shader, source);
			GL20.glCompileShader(shader);
			if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
				throw new IllegalStateException("Could not compile shader: " + GL20.glGetShaderInfoLog(shader));
			}
			return shader;
		}

		private static String readShader(String path) {
			try (InputStream in = Gpu.class.getResourceAsStream(path)) {
				if (in == null) {
					throw new IllegalStateException(path + " not found");
				}
				return new String(in.readAllBytes(), StandardCharsets.UTF_8);
			} catch (IOException e) {
				throw new IllegalStateException("Could not read " + path, e);
			}
		}
	}

	private static int emptyVertexArray;

	private static void bindVertexArray(int vertexArray) {
		//? if >=1.17 {
		GlStateManager._glBindVertexArray(vertexArray);
		//?} else {
		/*GL30.glBindVertexArray(vertexArray);
		*///?}
	}

	// Draws the pipeline over the whole of output, keeping its contents, and restores the GL state it touches.
	static void draw(Pipeline pipeline, RenderTarget output, Sampler... samplers) {
		if (pipeline.failed) {
			return;
		}
		if (pipeline.program == 0) {
			try {
				pipeline.compile();
			} catch (RuntimeException e) {
				// Leave the frame as it is instead of crashing the game; report it once.
				pipeline.failed = true;
				LitematicaPass.LOGGER.error("Could not set up {}; it will not be drawn", pipeline.fragmentShader, e);
				return;
			}
		}
		if (emptyVertexArray == 0) {
			//? if >=1.17 {
			emptyVertexArray = GlStateManager._glGenVertexArrays();
			//?} else {
			/*emptyVertexArray = GL30.glGenVertexArrays();
			*///?}
		}
		int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
		int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
		int[] viewport = new int[4];
		GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
		boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
		boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
		//? if >=1.17 {
		int activeTexture = GlStateManager._getActiveTexture();
		//?} else {
		/*int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
		*///?}
		// Minecraft caches the bound program, vertex array and textures; restore exactly what was bound, or its next
		// draw runs without its shader (depth only, no colour).
		int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
		int vertexArray = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
		int[] textures = new int[samplers.length];
		for (int i = 0; i < samplers.length; i++) {
			GlStateManager._activeTexture(GL13.GL_TEXTURE0 + i);
			textures[i] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		}

		GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, output.frameBufferId);
		GlStateManager._viewport(0, 0, output.width, output.height);
		if (pipeline.depthOnly) {
			GlStateManager._colorMask(false, false, false, false);
			GlStateManager._depthMask(true);
			GlStateManager._enableDepthTest();
			GlStateManager._depthFunc(GL11.GL_LEQUAL);
			GlStateManager._disableBlend();
		} else {
			GlStateManager._colorMask(true, true, true, false);
			GlStateManager._depthMask(false);
			GlStateManager._disableDepthTest();
			if (pipeline.premultiplied) {
				GlStateManager._enableBlend();
				GlStateManager._blendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
			} else {
				GlStateManager._disableBlend();
			}
		}
		GlStateManager._glUseProgram(pipeline.program);
		for (int i = 0; i < samplers.length; i++) {
			GlStateManager._activeTexture(GL13.GL_TEXTURE0 + i);
			GlStateManager._bindTexture(samplers[i].depth() ? samplers[i].target().getDepthTextureId() : samplers[i].target().getColorTextureId());
			GlStateManager._glUniform1i(pipeline.location(samplers[i].name()), i);
		}
		bindVertexArray(emptyVertexArray);
		GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);

		bindVertexArray(vertexArray);
		GlStateManager._glUseProgram(program);
		for (int i = samplers.length - 1; i >= 0; i--) {
			GlStateManager._activeTexture(GL13.GL_TEXTURE0 + i);
			GlStateManager._bindTexture(textures[i]);
		}
		GlStateManager._activeTexture(activeTexture);
		GlStateManager._colorMask(true, true, true, true);
		GlStateManager._depthMask(true);
		GlStateManager._depthFunc(GL11.GL_LEQUAL);
		if (depthTest) {
			GlStateManager._enableDepthTest();
		} else {
			GlStateManager._disableDepthTest();
		}
		if (blend) {
			GlStateManager._enableBlend();
		} else {
			GlStateManager._disableBlend();
		}
		GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
		GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
		GlStateManager._viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
	}

}
