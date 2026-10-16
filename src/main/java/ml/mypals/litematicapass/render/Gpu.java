package ml.mypals.litematicapass.render;

import ml.mypals.litematicapass.LitematicaPass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

/**
 * The GPU work of the mod, in plain OpenGL (Minecraft 1.12 has no render pipeline API): our own textures and
 * framebuffers, copies of the depth of the bound framebuffer, and full-screen passes ({@link #colorPipeline} /
 * {@link #depthPipeline} drawn with {@link #draw}). Every call restores the GL state it touches exactly, so
 * GlStateManager's caches stay true.
 *
 * <p>Unlike the newer versions, the textures involved need not have one size: with a shader pack OptiFine renders the
 * world at its own resolution (its render quality setting), so a pass samples each texture at the position matching
 * the pixel it writes.
 */
final class Gpu {
	/** The depth a cleared depth buffer holds (1.12: near = 0, far = 1; the shaders define STANDARD_DEPTH). */
	static final double FAR_DEPTH = 1.0;

	private static final IntBuffer VIEWPORT = BufferUtils.createIntBuffer(16);

	private Gpu() {}

	/** A texture of a given size: the colour or depth of one of our targets, or a texture we only copy into. */
	static final class Texture {
		int id;
		int width;
		int height;

		Texture(int id, int width, int height) {
			this.id = id;
			this.width = width;
			this.height = height;
		}
	}

	/** A framebuffer with a colour and a depth texture. */
	static final class Target {
		int framebuffer;
		final Texture color = new Texture(0, 0, 0);
		final Texture depth = new Texture(0, 0, 0);

		int width() {
			return this.color.width;
		}

		int height() {
			return this.color.height;
		}
	}

	/** Where a pass writes: a framebuffer and its size. */
	static final class Output {
		final int framebuffer;
		final int width;
		final int height;

		Output(int framebuffer, int width, int height) {
			this.framebuffer = framebuffer;
			this.width = width;
			this.height = height;
		}

		static Output of(Target target) {
			return new Output(target.framebuffer, target.width(), target.height());
		}
	}

	/** Minecraft's framebuffer, which OptiFine's final pass writes the shaded frame into; null if it has none. */
	static Output mainOutput() {
		Framebuffer main = Minecraft.getMinecraft().getFramebuffer();
		if (main == null || main.framebufferObject <= 0) {
			return null;
		}
		return new Output(main.framebufferObject, main.framebufferWidth, main.framebufferHeight);
	}

	// Textures

	private static void allocate(Texture texture, boolean depth, int width, int height) {
		int saved = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		if (texture.id == 0) {
			texture.id = GL11.glGenTextures();
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.id);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12_CLAMP_TO_EDGE);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12_CLAMP_TO_EDGE);
			if (depth) {
				GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_MODE, GL11.GL_NONE);
			}
		} else {
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.id);
		}
		if (depth) {
			GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL14.GL_DEPTH_COMPONENT24, width, height, 0, GL11.GL_DEPTH_COMPONENT,
				GL11.GL_UNSIGNED_INT, (ByteBuffer) null);
		} else {
			GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE,
				(ByteBuffer) null);
		}
		texture.width = width;
		texture.height = height;
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, saved);
	}

	private static final int GL12_CLAMP_TO_EDGE = 0x812F;

	/** A depth texture we copy into; reallocated when the size changes. */
	static void ensureDepthTexture(Texture texture, int width, int height) {
		if (texture.id == 0 || texture.width != width || texture.height != height) {
			allocate(texture, true, width, height);
		}
	}

	/** Creates or resizes a colour + depth target. */
	static void ensureTarget(Target target, int width, int height) {
		if (target.framebuffer != 0 && target.width() == width && target.height() == height) {
			return;
		}
		allocate(target.color, false, width, height);
		allocate(target.depth, true, width, height);
		if (target.framebuffer == 0) {
			int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
			int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
			target.framebuffer = GL30.glGenFramebuffers();
			GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, target.framebuffer);
			GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, target.color.id, 0);
			GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, target.depth.id, 0);
			int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
			if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
				LitematicaPass.LOGGER.error("The schematic framebuffer is incomplete (status 0x{})", Integer.toHexString(status));
			}
			GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
		}
	}

	static void delete(Texture texture) {
		if (texture.id != 0) {
			GL11.glDeleteTextures(texture.id);
			texture.id = 0;
		}
	}

	static void delete(Target target) {
		if (target.framebuffer != 0) {
			GL30.glDeleteFramebuffers(target.framebuffer);
			target.framebuffer = 0;
		}
		delete(target.color);
		delete(target.depth);
	}

	/** Clears the colour of {@code target} to transparent black and its depth to {@link #FAR_DEPTH}. */
	static void clear(Target target) {
		Runnable restore = bindForDrawing(target);
		FloatBuffer values = BufferUtils.createFloatBuffer(16);
		GL11.glGetFloat(GL11.GL_COLOR_CLEAR_VALUE, values);
		float[] clearColor = { values.get(0), values.get(1), values.get(2), values.get(3) };
		values.clear();
		GL11.glGetFloat(GL11.GL_DEPTH_CLEAR_VALUE, values);
		double clearDepth = values.get(0);
		boolean[] colorMask = colorMask();
		boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
		boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);

		GL11.glColorMask(true, true, true, true);
		GL11.glDepthMask(true);
		GL11.glDisable(GL11.GL_SCISSOR_TEST);
		GL11.glClearColor(0, 0, 0, 0);
		GL11.glClearDepth(FAR_DEPTH);
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

		GL11.glClearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
		GL11.glClearDepth(clearDepth);
		GL11.glColorMask(colorMask[0], colorMask[1], colorMask[2], colorMask[3]);
		GL11.glDepthMask(depthMask);
		setEnabled(GL11.GL_SCISSOR_TEST, scissor);
		restore.run();
	}

	// The bound framebuffer

	/**
	 * The depth texture of the framebuffer bound for {@code target} (GL_DRAW_FRAMEBUFFER or GL_READ_FRAMEBUFFER), with its
	 * size, or null if it is the default framebuffer or its depth is not a texture.
	 */
	static Texture boundDepthTexture(int target) {
		int framebuffer = GL11.glGetInteger(target == GL30.GL_READ_FRAMEBUFFER ? GL30.GL_READ_FRAMEBUFFER_BINDING : GL30.GL_DRAW_FRAMEBUFFER_BINDING);
		if (framebuffer == 0) {
			return null;
		}
		int type = GL30.glGetFramebufferAttachmentParameteri(target, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE);
		if (type != GL11.GL_TEXTURE) {
			return null;
		}
		int texture = GL30.glGetFramebufferAttachmentParameteri(target, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME);
		int saved = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
		int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
		int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, saved);
		return width > 0 && height > 0 ? new Texture(texture, width, height) : null;
	}

	/**
	 * Copies the depth of the framebuffer bound for reading, of {@code source}'s size, into {@code copy} (the same
	 * copy OptiFine itself makes for its {@code depthtex1}).
	 */
	static void copyBoundDepth(Texture source, Texture copy) {
		ensureDepthTexture(copy, source.width, source.height);
		int saved = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, copy.id);
		GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, source.width, source.height);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, saved);
	}

	/**
	 * Makes {@code target} the framebuffer that following draws go to, with a viewport over all of it; run the
	 * returned action to bind back what was bound. Minecraft 1.12 draws into whatever framebuffer is bound.
	 */
	static Runnable bindForDrawing(Target target) {
		int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
		int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
		int[] viewport = viewport();
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, target.framebuffer);
		GL11.glViewport(0, 0, target.width(), target.height());
		return () -> {
			GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
			GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
		};
	}

	private static int[] viewport() {
		VIEWPORT.clear();
		GL11.glGetInteger(GL11.GL_VIEWPORT, VIEWPORT);
		return new int[] { VIEWPORT.get(0), VIEWPORT.get(1), VIEWPORT.get(2), VIEWPORT.get(3) };
	}

	private static boolean[] colorMask() {
		ByteBuffer mask = BufferUtils.createByteBuffer(16);
		GL11.glGetBoolean(GL11.GL_COLOR_WRITEMASK, mask);
		return new boolean[] { mask.get(0) != 0, mask.get(1) != 0, mask.get(2) != 0, mask.get(3) != 0 };
	}

	private static void setEnabled(int capability, boolean enabled) {
		if (enabled) {
			GL11.glEnable(capability);
		} else {
			GL11.glDisable(capability);
		}
	}

	// Full-screen passes

	/** A texture a full-screen pass samples, by the name of its sampler uniform. */
	static final class Sampler {
		final String name;
		final Texture texture;

		Sampler(String name, Texture texture) {
			this.name = name;
			this.texture = texture;
		}
	}

	static Sampler sampler(String name, Texture texture) {
		return new Sampler(name, texture);
	}

	/**
	 * A full-screen pass writing RGB (not alpha) into a framebuffer's colour, without depth; {@code premultiplied}
	 * blends premultiplied colour over it, otherwise it replaces it.
	 */
	static Pipeline colorPipeline(String fragmentShader, boolean premultiplied, Map<String, Integer> defines, String... samplers) {
		return new Pipeline(fragmentShader, false, premultiplied, defines, samplers);
	}

	/** A full-screen pass writing only a framebuffer's depth, with the default depth test. */
	static Pipeline depthPipeline(String fragmentShader, String... samplers) {
		return new Pipeline(fragmentShader, true, false, Collections.<String, Integer>emptyMap(), samplers);
	}

	static final class Pipeline {
		// One triangle over the whole screen, from a buffer: legacy (compatibility) contexts, which 1.12 runs in, need
		// an enabled attribute array to draw anything.
		private static final String VERTEX_SHADER = "#version 330\n"
			+ "layout(location = 0) in vec2 Position;\n"
			+ "void main() {\n"
			+ "    gl_Position = vec4(Position, 0.0, 1.0);\n"
			+ "}\n";

		private final String fragmentShader;
		private final boolean depthOnly;
		private final boolean premultiplied;
		private final Map<String, Integer> defines;
		private final String[] samplers;
		private int program;
		private int[] samplerLocations;
		private int outputSizeLocation;
		private boolean failed;

		private Pipeline(String fragmentShader, boolean depthOnly, boolean premultiplied, Map<String, Integer> defines,
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
			for (Map.Entry<String, Integer> define : this.defines.entrySet()) {
				header.append("#define ").append(define.getKey()).append(' ').append(define.getValue()).append('\n');
			}
			int afterVersion = source.indexOf('\n') + 1;
			source = source.substring(0, afterVersion) + header + source.substring(afterVersion);

			int vertex = compileShader(GL20.GL_VERTEX_SHADER, VERTEX_SHADER);
			int fragment = compileShader(GL20.GL_FRAGMENT_SHADER, source);
			int program = GL20.glCreateProgram();
			GL20.glAttachShader(program, vertex);
			GL20.glAttachShader(program, fragment);
			GL20.glBindAttribLocation(program, 0, "Position");
			GL30.glBindFragDataLocation(program, 0, "fragColor");
			GL20.glLinkProgram(program);
			GL20.glDeleteShader(vertex);
			GL20.glDeleteShader(fragment);
			if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
				throw new IllegalStateException("Could not link " + this.fragmentShader + ": " + GL20.glGetProgramInfoLog(program, 4096));
			}
			this.samplerLocations = new int[this.samplers.length];
			for (int i = 0; i < this.samplers.length; i++) {
				this.samplerLocations[i] = GL20.glGetUniformLocation(program, this.samplers[i]);
			}
			this.outputSizeLocation = GL20.glGetUniformLocation(program, "OutputSize");
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
			// LWJGL 2 turns a CharSequence into bytes by keeping each char's low byte, so the Chinese comments of the
			// shaders came out as garbage, some of it NUL, and the driver saw the source end early. GLSL is ASCII anyway:
			// blank anything else (only comments have it) and hand over the bytes.
			byte[] bytes = source.replaceAll("[^\\x00-\\x7F]", " ").getBytes(StandardCharsets.US_ASCII);
			ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
			buffer.put(bytes).flip();
			GL20.glShaderSource(shader, buffer);
			GL20.glCompileShader(shader);
			if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
				throw new IllegalStateException("Could not compile shader: " + GL20.glGetShaderInfoLog(shader, 4096));
			}
			return shader;
		}

		private static String readShader(String path) {
			try (InputStream in = Gpu.class.getResourceAsStream(path)) {
				if (in == null) {
					throw new IllegalStateException(path + " not found");
				}
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				byte[] buffer = new byte[4096];
				int read;
				while ((read = in.read(buffer)) != -1) {
					out.write(buffer, 0, read);
				}
				return new String(out.toByteArray(), StandardCharsets.UTF_8);
			} catch (IOException e) {
				throw new IllegalStateException("Could not read " + path, e);
			}
		}
	}

	private static int vertexArray;
	private static int vertexBuffer;

	private static void ensureTriangle() {
		if (vertexArray != 0) {
			return;
		}
		int savedArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
		int savedVertexArray = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
		vertexArray = GL30.glGenVertexArrays();
		vertexBuffer = GL15.glGenBuffers();
		GL30.glBindVertexArray(vertexArray);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBuffer);
		FloatBuffer corners = BufferUtils.createFloatBuffer(6);
		corners.put(new float[] { -1, -1, 3, -1, -1, 3 }).flip();
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, corners, GL15.GL_STATIC_DRAW);
		GL20.glEnableVertexAttribArray(0);
		GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 0, 0L);
		GL30.glBindVertexArray(savedVertexArray);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, savedArrayBuffer);
	}

	// Draws the pipeline over the whole of output, keeping its contents, and restores the GL state it touches.
	static void draw(Pipeline pipeline, Output output, Sampler... samplers) {
		if (pipeline.failed) {
			return;
		}
		if (pipeline.program == 0) {
			try {
				pipeline.compile();
				ensureTriangle();
			} catch (RuntimeException e) {
				// Leave the frame as it is instead of crashing the game; report it once.
				pipeline.failed = true;
				LitematicaPass.LOGGER.error("Could not set up {}; it will not be drawn", pipeline.fragmentShader, e);
				return;
			}
		}
		int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
		int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
		int[] viewport = viewport();
		boolean[] colorMask = colorMask();
		boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
		int depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
		int blendSrcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
		int blendDstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
		int blendSrcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
		int blendDstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
		// The fixed-function per-fragment tests still apply to programs in a compatibility context.
		int[] capabilities = { GL11.GL_DEPTH_TEST, GL11.GL_BLEND, GL11.GL_ALPHA_TEST, GL11.GL_CULL_FACE, GL11.GL_SCISSOR_TEST,
			GL11.GL_STENCIL_TEST, GL11.GL_POLYGON_OFFSET_FILL };
		boolean[] enabled = new boolean[capabilities.length];
		for (int i = 0; i < capabilities.length; i++) {
			enabled[i] = GL11.glIsEnabled(capabilities[i]);
			GL11.glDisable(capabilities[i]);
		}
		int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
		int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
		int savedVertexArray = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
		int[] textures = new int[samplers.length];
		for (int i = 0; i < samplers.length; i++) {
			GL13.glActiveTexture(GL13.GL_TEXTURE0 + i);
			textures[i] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		}

		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, output.framebuffer);
		GL11.glViewport(0, 0, output.width, output.height);
		if (pipeline.depthOnly) {
			GL11.glColorMask(false, false, false, false);
			GL11.glDepthMask(true);
			GL11.glEnable(GL11.GL_DEPTH_TEST);
			GL11.glDepthFunc(GL11.GL_LEQUAL);
		} else {
			GL11.glColorMask(true, true, true, false);
			GL11.glDepthMask(false);
			if (pipeline.premultiplied) {
				GL11.glEnable(GL11.GL_BLEND);
				GL14.glBlendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
			}
		}
		GL20.glUseProgram(pipeline.program);
		if (pipeline.outputSizeLocation >= 0) {
			GL20.glUniform2f(pipeline.outputSizeLocation, output.width, output.height);
		}
		for (int i = 0; i < samplers.length; i++) {
			GL13.glActiveTexture(GL13.GL_TEXTURE0 + i);
			// A texture that never existed this session (no scene depth seen) samples as texture 0.
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, samplers[i].texture != null ? samplers[i].texture.id : 0);
			GL20.glUniform1i(pipeline.location(samplers[i].name), i);
		}
		GL30.glBindVertexArray(vertexArray);
		GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);

		GL30.glBindVertexArray(savedVertexArray);
		GL20.glUseProgram(program);
		for (int i = samplers.length - 1; i >= 0; i--) {
			GL13.glActiveTexture(GL13.GL_TEXTURE0 + i);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, textures[i]);
		}
		GL13.glActiveTexture(activeTexture);
		GL11.glColorMask(colorMask[0], colorMask[1], colorMask[2], colorMask[3]);
		GL11.glDepthMask(depthMask);
		GL11.glDepthFunc(depthFunc);
		GL14.glBlendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);
		for (int i = 0; i < capabilities.length; i++) {
			setEnabled(capabilities[i], enabled[i]);
		}
		GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
		GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
		GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
		int error = GL11.glGetError();
		if (error != GL11.GL_NO_ERROR) {
			Stats.count("GL error 0x" + Integer.toHexString(error) + " after " + pipeline.fragmentShader);
		}
	}
}
