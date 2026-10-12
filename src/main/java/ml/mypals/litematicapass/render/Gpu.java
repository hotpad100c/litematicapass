package ml.mypals.litematicapass.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import ml.mypals.litematicapass.LitematicaPass;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;

import java.util.Map;
//? if >=1.21.5 {
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;

import java.util.Optional;
import java.util.OptionalDouble;
//?} else {
/*import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
*///?}
//? if >=1.21.6 {
import com.mojang.renderpearl.api.textures.GpuTextureView;
//?} else if >=1.21.5 {
/*import com.mojang.blaze3d.textures.GpuTexture;
*///?}
//? if >=1.21.11 {
import com.mojang.renderpearl.api.textures.FilterMode;
//?}
//? if <1.21.6 {
/*import ml.mypals.litematicapass.mixin.MinecraftAccessor;
*///?}
//? if <1.21.6 && >=1.21.5 {
/*import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
*///?}
//? if >=26.2 {
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.commands.RenderPassDescriptor;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import org.joml.Vector4f;
//?} else if >=1.21.5 {
/*import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.OptionalInt;
*///?}
//? if >=26.3 {
import com.mojang.renderpearl.api.pipeline.UniformType;
//?}
//? if >=26.1 {
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
//?} else if >=1.21.5 {
/*import com.mojang.blaze3d.platform.DepthTestFunction;
*///?}

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
	//? if >=26.2 {
	static final double FAR_DEPTH = 0.0;
	//?} else {
	/*static final double FAR_DEPTH = 1.0;
	*///?}

	private Gpu() {}

	static RenderTarget mainTarget() {
		//? if >=26.2 {
		return Minecraft.getInstance().gameRenderer.mainRenderTarget();
		//?} else if >=1.21.6 {
		/*return Minecraft.getInstance().getMainRenderTarget();
		*///?} else {
		/*// Before 1.21.6 routing redirects getMainRenderTarget() (see MinecraftMixin); read the field itself.
		return ((MinecraftAccessor) Minecraft.getInstance()).litematicapass$mainRenderTarget();
		*///?}
	}

	/** A colour + depth target. */
	static TextureTarget colorDepthTarget(String label, int width, int height) {
		//? if >=26.3 {
		return new TextureTarget(label, width, height, GpuFormat.RGBA8_UNORM, GpuFormat.D32_FLOAT);
		//?} else if >=26.2 {
		/*return new TextureTarget(label, width, height, true, GpuFormat.RGBA8_UNORM);
		*///?} else if >=1.21.5 {
		/*return new TextureTarget(label, width, height, true);
		*///?} else if >=1.21.2 {
		/*TextureTarget target = new TextureTarget(width, height, true);
		target.setClearColor(0, 0, 0, 0);
		return target;
		*///?} else {
		/*TextureTarget target = new TextureTarget(width, height, true, Minecraft.ON_OSX);
		target.setClearColor(0, 0, 0, 0);
		return target;
		*///?}
	}

	static void resize(RenderTarget target, int width, int height) {
		//? if >=1.21.2 {
		target.resize(width, height);
		//?} else {
		/*target.resize(width, height, Minecraft.ON_OSX);
		*///?}
	}

	/** A target whose depth is used; older versions always give it a colour texture too. */
	static TextureTarget depthTarget(String label, int width, int height) {
		//? if >=26.3 {
		return new TextureTarget(label, width, height, null, GpuFormat.D32_FLOAT);
		//?} else {
		/*return colorDepthTarget(label, width, height);
		*///?}
	}

	/** Clears the colour to transparent black and the depth to {@link #FAR_DEPTH}. */
	static void clear(RenderTarget target) {
		//? if >=26.2 {
		RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
			target.getColorTexture(), new Vector4f(0, 0, 0, 0), target.getDepthTexture(), FAR_DEPTH);
		//?} else if >=1.21.5 {
		/*RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
			target.getColorTexture(), 0, target.getDepthTexture(), FAR_DEPTH);
		*///?} else {
		/*// RenderTarget.clear() leaves the default framebuffer bound; keep whatever was bound.
		int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
		int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
		int[] viewport = new int[4];
		GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
		clearUnbound(target);
		GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
		GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
		GlStateManager._viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
		*///?}
	}

	//? if <1.21.5 && >=1.21.2 {
	/*private static void clearUnbound(RenderTarget target) {
		target.clear();
	}
	*///?} else if <1.21.2 {
	/*private static void clearUnbound(RenderTarget target) {
		target.clear(Minecraft.ON_OSX);
	}
	*///?}

	/** Copies the depth of {@code source} into {@code target}. */
	static void copyDepth(RenderTarget target, RenderTarget source) {
		//? if >=1.21.5 {
		target.copyDepthFrom(source);
		//?} else {
		/*// copyDepthFrom leaves the default framebuffer bound; this runs inside the shader pack's passes.
		int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
		int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
		target.copyDepthFrom(source);
		GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
		GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
		*///?}
	}

	/**
	 * Points the output of RenderType draws at {@code target}; run the returned action to undo it. 26.3 draws features
	 * into a pass we open ourselves, and before 1.21.6 there is no such override, but while routing the schematic target
	 * is handed out as the main target (MinecraftMixin).
	 */
	static Runnable redirectOutput(RenderTarget target) {
		//? if <26.3 && >=1.21.6 {
		/*GpuTextureView savedColor = RenderSystem.outputColorTextureOverride;
		GpuTextureView savedDepth = RenderSystem.outputDepthTextureOverride;
		RenderSystem.outputColorTextureOverride = target.getColorTextureView();
		RenderSystem.outputDepthTextureOverride = target.getDepthTextureView();
		return () -> {
			RenderSystem.outputColorTextureOverride = savedColor;
			RenderSystem.outputDepthTextureOverride = savedDepth;
		};
		*///?} else {
		return () -> {};
		//?}
	}

	/**
	 * Makes {@code target} the framebuffer that following draws go to; run the returned action to bind back what was
	 * bound. Only before 1.21.5: there draws go to whatever framebuffer is bound (on 1.21.1 render types do not even
	 * bind the main target), while later versions name the target of every pass.
	 */
	static Runnable bindForDrawing(RenderTarget target) {
		//? if >=1.21.5 {
		return () -> {};
		//?} else {
		/*int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
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
		*///?}
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

	//? if >=1.21.5 {
	static final class Pipeline {
		private final boolean depthOnly;
		private final String label;
		private final RenderPipeline pipeline;

		private Pipeline(String location, String fragmentShader, boolean depthOnly, boolean premultiplied, Map<String, Integer> defines,
			String[] samplers) {
			this.depthOnly = depthOnly;
			this.label = LitematicaPass.MOD_ID + "/" + location;
			RenderPipeline.Builder builder = screenQuad(location, fragmentShader, samplers);
			defines.forEach(builder::withShaderDefine);
			if (depthOnly) {
				depthOnly(builder);
			} else {
				colorOutput(builder, premultiplied ? Optional.of(PREMULTIPLIED_ALPHA) : Optional.empty());
			}
			this.pipeline = RenderPipelines.register(builder.build());
		}
	}

	/** Draws the pipeline over the whole of {@code output}, keeping its contents. */
	static void draw(Pipeline pipeline, RenderTarget output, Sampler... samplers) {
		try (RenderPass pass = pipeline.depthOnly ? depthPass(pipeline.label, output) : colorPass(pipeline.label, output)) {
			//? if >=26.3 {
			pass.setPipeline(RenderSystem.getCompiledPipeline(pipeline.pipeline));
			//?} else {
			/*pass.setPipeline(pipeline.pipeline);
			*///?}
			//? if >=1.21.6 {
			RenderSystem.bindDefaultUniforms(pass);
			//?}
			for (Sampler sampler : samplers) {
				bind(pass, sampler.name(), sampler.depth() ? depth(sampler.target()) : color(sampler.target()));
			}
			drawScreenQuad(pass);
		}
	}

	/** Blending for premultiplied colour (1.21.6 added it to Minecraft; the same factors before). */
	//? if >=1.21.6 {
	private static final BlendFunction PREMULTIPLIED_ALPHA = BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA;
	//?} else {
	/*private static final BlendFunction PREMULTIPLIED_ALPHA = new BlendFunction(
		SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA);
	*///?}

	/**
	 * A pipeline covering the screen, sampling the given textures: one triangle with {@code core/screenquad}, or before
	 * 1.21.9 (which added that shader) the shared quad with {@code core/blit_screen}.
	 */
	private static RenderPipeline.Builder screenQuad(String location, String fragmentShader, String... samplers) {
		RenderPipeline.Builder builder = RenderPipeline.builder()
			.withLocation(LitematicaPass.id(location))
			//? if >=1.21.9 {
			.withVertexShader("core/screenquad")
			//?} else {
			/*.withVertexShader("core/blit_screen")
			*///?}
			.withFragmentShader(LitematicaPass.id(fragmentShader));
		//? if >=26.2 {
		BindGroupLayout.Builder layout = BindGroupLayout.builder();
		for (String name : samplers) {
			//? if >=26.3 {
			layout.withUniform(name, UniformType.COMBINED_IMAGE_SAMPLER);
			//?} else {
			/*layout.withSampler(name);
			*///?}
		}
		builder.withBindGroupLayout(layout.build()).withPrimitiveTopology(PrimitiveTopology.TRIANGLES);
		//?} else {
		/*for (String name : samplers) {
			builder.withSampler(name);
		}
		builder.withShaderDefine("STANDARD_DEPTH");
		*///?}
		//? if <26.2 && >=1.21.9 {
		/*builder.withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES);
		*///?} else if <1.21.9 {
		/*builder.withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS);
		*///?}
		return builder;
	}

	/** Makes the pipeline write RGB (not alpha) into the target, blended as given, without depth. */
	private static void colorOutput(RenderPipeline.Builder builder, Optional<BlendFunction> blend) {
		//? if >=26.2 {
		builder.withColorTargetState(new ColorTargetState(blend, GpuFormat.RGBA8_UNORM, ColorTargetState.WRITE_COLOR));
		//?} else if >=26.1 {
		/*builder.withColorTargetState(new ColorTargetState(blend, ColorTargetState.WRITE_COLOR));
		*///?} else {
		/*blend.ifPresentOrElse(builder::withBlend, builder::withoutBlend);
		builder.withColorWrite(true, false).withDepthWrite(false).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST);
		*///?}
	}

	/**
	 * Makes a pipeline that only writes depth, with the default depth test. Before 26.3 a pipeline without colour
	 * targets still gets a default one (and a pass needs a colour attachment before 26.2), so the pass attaches the
	 * target's colour and the pipeline masks it off (see {@link #depthPass}).
	 */
	private static void depthOnly(RenderPipeline.Builder builder) {
		//? if >=26.3 {
		builder.withDepthStencilState(DepthStencilState.DEFAULT);
		//?} else if >=26.2 {
		/*builder.withColorTargetState(new ColorTargetState(Optional.empty(), GpuFormat.RGBA8_UNORM, ColorTargetState.WRITE_NONE))
			.withDepthStencilState(DepthStencilState.DEFAULT);
		*///?} else if >=26.1 {
		/*builder.withColorTargetState(new ColorTargetState(Optional.empty(), ColorTargetState.WRITE_NONE))
			.withDepthStencilState(DepthStencilState.DEFAULT);
		*///?} else {
		/*builder.withColorWrite(false, false).withDepthWrite(true).withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST);
		*///?}
	}

	/** Opens a pass drawing into the colour of {@code target}, keeping its contents. */
	private static RenderPass colorPass(String label, RenderTarget target) {
		return openPass(label, color(target), null, target);
	}

	/**
	 * Opens a pass drawing into the depth of {@code target}, keeping its contents. Before 26.3 the target's colour is
	 * attached too, unwritten (see {@link #depthOnly}).
	 */
	private static RenderPass depthPass(String label, RenderTarget target) {
		//? if >=26.3 {
		return openPass(label, null, depth(target), target);
		//?} else {
		/*return openPass(label, color(target), depth(target), target);
		*///?}
	}

	/** Draws the full-screen triangle (or before 1.21.9, quad) of a {@link #screenQuad} pipeline. */
	private static void drawScreenQuad(RenderPass pass) {
		//? if >=26.2 {
		pass.draw(3, 1, 0, 0);
		//?} else if >=1.21.9 {
		/*pass.draw(0, 3);
		*///?} else {
		/*RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
		pass.setVertexBuffer(0, RenderSystem.getQuadVertexBuffer());
		pass.setIndexBuffer(indices.getBuffer(6), indices.type());
		drawQuadIndices(pass);
		*///?}
	}

	//? if <1.21.9 && >=1.21.6 {
	/*private static void drawQuadIndices(RenderPass pass) {
		pass.drawIndexed(0, 0, 6, 1);
	}
	*///?} else if <1.21.6 && >=1.21.5 {
	/*private static void drawQuadIndices(RenderPass pass) {
		pass.drawIndexed(0, 6);
	}
	*///?}
	//?} else {
	/*// Before 1.21.5 there is no RenderPipeline / RenderPass API: compile the shaders and draw with OpenGL.
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
			emptyVertexArray = GlStateManager._glGenVertexArrays();
		}
		int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
		int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
		int[] viewport = new int[4];
		GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
		boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
		boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
		int activeTexture = GlStateManager._getActiveTexture();
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
		GlStateManager._glBindVertexArray(emptyVertexArray);
		GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);

		GlStateManager._glBindVertexArray(vertexArray);
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
	*///?}

	//? if >=1.21.6 {
	private static GpuTextureView color(RenderTarget target) {
		return target.getColorTextureView();
	}

	private static GpuTextureView depth(RenderTarget target) {
		return target.getDepthTextureView();
	}

	private static RenderPass openPass(String label, @Nullable GpuTextureView color, @Nullable GpuTextureView depth, RenderTarget size) {
		//? if >=26.3 {
		RenderPassDescriptor.Builder descriptor = RenderPassDescriptor.builder(() -> label);
		//?} else if >=26.2 {
		/*RenderPassDescriptor descriptor = RenderPassDescriptor.create(() -> label);
		*///?}
		//? if >=26.2 {
		if (color != null) {
			descriptor.withColorAttachment(color);
		}
		if (depth != null) {
			descriptor.withDepthAttachment(depth, OptionalDouble.empty());
		}
		descriptor.withRenderArea(new RenderPass.RenderArea(0, 0, size.width, size.height));
		//?}
		//? if >=26.3 {
		return RenderSystem.getDevice().createCommandEncoder().createRenderPass(descriptor.build());
		//?} else if >=26.2 {
		/*return RenderSystem.getDevice().createCommandEncoder().createRenderPass(descriptor);
		*///?} else {
		/*return RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> label, color, OptionalInt.empty(), depth, OptionalDouble.empty());
		*///?}
	}

	private static void bind(RenderPass pass, String name, GpuTextureView view) {
		//? if >=26.3 {
		pass.setUniform(name, view, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
		//?} else if >=1.21.11 {
		/*pass.bindTexture(name, view, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
		*///?} else {
		/*pass.bindSampler(name, view);
		*///?}
	}
	//?} else if >=1.21.5 {
	/*// Before 1.21.6 passes and samplers take the textures themselves (there are no texture views).
	private static GpuTexture color(RenderTarget target) {
		return target.getColorTexture();
	}

	private static GpuTexture depth(RenderTarget target) {
		return target.getDepthTexture();
	}

	private static RenderPass openPass(String label, @Nullable GpuTexture color, @Nullable GpuTexture depth, RenderTarget size) {
		return RenderSystem.getDevice().createCommandEncoder().createRenderPass(color, OptionalInt.empty(), depth, OptionalDouble.empty());
	}

	private static void bind(RenderPass pass, String name, GpuTexture texture) {
		pass.bindSampler(name, texture);
	}
	*///?}
}
