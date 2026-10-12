package ml.mypals.litematicapass.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
//? if >=1.21.6 {
import com.mojang.renderpearl.api.textures.GpuTextureView;
//?} else {
/*import com.mojang.blaze3d.textures.GpuTexture;
*///?}
//? if >=1.21.11 {
import com.mojang.renderpearl.api.textures.FilterMode;
//?}
import ml.mypals.litematicapass.LitematicaPass;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;
//? if <1.21.6 {
/*import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import ml.mypals.litematicapass.mixin.MinecraftAccessor;
*///?}
//? if >=26.2 {
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.commands.RenderPassDescriptor;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import org.joml.Vector4f;
//?} else {
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
//?} else {
/*import com.mojang.blaze3d.platform.DepthTestFunction;
*///?}

import java.util.Optional;
import java.util.OptionalDouble;

/** The GPU calls whose shape differs between Minecraft versions, so the render classes can share one body. */
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

	/** Blending for premultiplied colour (1.21.6 added it to Minecraft; the same factors before). */
	//? if >=1.21.6 {
	static final BlendFunction PREMULTIPLIED_ALPHA = BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA;
	//?} else {
	/*static final BlendFunction PREMULTIPLIED_ALPHA = new BlendFunction(
		SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA);
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
		*///?} else {
		/*return new TextureTarget(label, width, height, true);
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
		//?} else {
		/*RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
			target.getColorTexture(), 0, target.getDepthTexture(), FAR_DEPTH);
		*///?}
	}

	/**
	 * A pipeline covering the screen, sampling the given textures: one triangle with {@code core/screenquad}, or before
	 * 1.21.6 the shared quad with {@code core/blit_screen}.
	 */
	static RenderPipeline.Builder screenQuad(String location, String fragmentShader, String... samplers) {
		RenderPipeline.Builder builder = RenderPipeline.builder()
			.withLocation(LitematicaPass.id(location))
			//? if >=1.21.6 {
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
		//? if <26.2 && >=1.21.6 {
		/*builder.withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES);
		*///?} else if <1.21.6 {
		/*builder.withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS);
		*///?}
		return builder;
	}

	/** Makes the pipeline write RGB (not alpha) into the main target, blended as given, without depth. */
	static RenderPipeline.Builder colorOutput(RenderPipeline.Builder builder, Optional<BlendFunction> blend) {
		//? if >=26.2 {
		return builder.withColorTargetState(new ColorTargetState(blend, GpuFormat.RGBA8_UNORM, ColorTargetState.WRITE_COLOR));
		//?} else if >=26.1 {
		/*return builder.withColorTargetState(new ColorTargetState(blend, ColorTargetState.WRITE_COLOR));
		*///?} else {
		/*blend.ifPresentOrElse(builder::withBlend, builder::withoutBlend);
		return builder.withColorWrite(true, false).withDepthWrite(false).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST);
		*///?}
	}

	/**
	 * Makes a pipeline that only writes depth, with the default depth test. Before 26.2 every pass needs a colour
	 * attachment and a pipeline writes all colour channels unless told otherwise, so mask them off (see
	 * {@link #depthPass}).
	 */
	static RenderPipeline.Builder depthOnly(RenderPipeline.Builder builder) {
		//? if >=26.2 {
		return builder.withDepthStencilState(DepthStencilState.DEFAULT);
		//?} else if >=26.1 {
		/*return builder.withColorTargetState(new ColorTargetState(Optional.empty(), ColorTargetState.WRITE_NONE))
			.withDepthStencilState(DepthStencilState.DEFAULT);
		*///?} else {
		/*return builder.withColorWrite(false, false).withDepthWrite(true).withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST);
		*///?}
	}

	/** Opens a pass drawing into the colour of {@code target}, keeping its contents. */
	static RenderPass colorPass(String label, RenderTarget target) {
		return openPass(label, color(target), null, target);
	}

	/**
	 * Opens a pass drawing into the depth of {@code target}, keeping its contents. Before 26.2 every pass needs a colour
	 * attachment, so the target's colour is attached too (see {@link #depthOnly}).
	 */
	static RenderPass depthPass(String label, RenderTarget target) {
		//? if >=26.2 {
		return openPass(label, null, depth(target), target);
		//?} else {
		/*return openPass(label, color(target), depth(target), target);
		*///?}
	}

	/** Binds the colour of {@code target}; the shaders read it with {@code texelFetch}, so filtering does not matter. */
	static void bindColor(RenderPass pass, String name, RenderTarget target) {
		bind(pass, name, color(target));
	}

	/** Binds the depth of {@code target}; the shaders read it with {@code texelFetch}, so filtering does not matter. */
	static void bindDepth(RenderPass pass, String name, RenderTarget target) {
		bind(pass, name, depth(target));
	}

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
	//?} else {
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

	/** Sets the pipeline and binds the default uniforms (1.21.6 and later have them). */
	static void setPipeline(RenderPass pass, RenderPipeline pipeline) {
		//? if >=26.3 {
		pass.setPipeline(RenderSystem.getCompiledPipeline(pipeline));
		//?} else {
		/*pass.setPipeline(pipeline);
		*///?}
		//? if >=1.21.6 {
		RenderSystem.bindDefaultUniforms(pass);
		//?}
	}

	/** Draws the full-screen triangle of a {@link #screenQuad} pipeline. */
	static void drawScreenQuad(RenderPass pass) {
		//? if >=26.2 {
		pass.draw(3, 1, 0, 0);
		//?} else if >=1.21.6 {
		/*pass.draw(0, 3);
		*///?} else {
		/*RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
		pass.setVertexBuffer(0, RenderSystem.getQuadVertexBuffer());
		pass.setIndexBuffer(indices.getBuffer(6), indices.type());
		pass.drawIndexed(0, 6);
		*///?}
	}
}
