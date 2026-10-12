package ml.mypals.litematicapass.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import ml.mypals.litematicapass.LitematicaPass;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;
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

	private Gpu() {}

	static RenderTarget mainTarget() {
		//? if >=26.2 {
		return Minecraft.getInstance().gameRenderer.mainRenderTarget();
		//?} else {
		/*return Minecraft.getInstance().getMainRenderTarget();
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

	/** A pipeline drawing one full-screen triangle with {@code core/screenquad}, sampling the given textures. */
	static RenderPipeline.Builder screenQuad(String location, String fragmentShader, String... samplers) {
		RenderPipeline.Builder builder = RenderPipeline.builder()
			.withLocation(LitematicaPass.id(location))
			.withVertexShader("core/screenquad")
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
		builder.withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES).withShaderDefine("STANDARD_DEPTH");
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
	 * {@link #depthPassColor}).
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

	/** The colour attachment of a depth-only pass on {@code target}: none from 26.2, its (unwritten) colour before. */
	static @Nullable GpuTextureView depthPassColor(RenderTarget target) {
		//? if >=26.2 {
		return null;
		//?} else {
		/*return target.getColorTextureView();
		*///?}
	}

	/** Opens a pass over the whole of the given attachments, keeping their contents. */
	static RenderPass openPass(String label, @Nullable GpuTextureView color, @Nullable GpuTextureView depth, int width, int height) {
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
		descriptor.withRenderArea(new RenderPass.RenderArea(0, 0, width, height));
		//?}
		//? if >=26.3 {
		return RenderSystem.getDevice().createCommandEncoder().createRenderPass(descriptor.build());
		//?} else if >=26.2 {
		/*return RenderSystem.getDevice().createCommandEncoder().createRenderPass(descriptor);
		*///?} else {
		/*return RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> label, color, OptionalInt.empty(), depth, OptionalDouble.empty());
		*///?}
	}

	static void setPipeline(RenderPass pass, RenderPipeline pipeline) {
		//? if >=26.3 {
		pass.setPipeline(RenderSystem.getCompiledPipeline(pipeline));
		//?} else {
		/*pass.setPipeline(pipeline);
		*///?}
	}

	static void bindTexture(RenderPass pass, String name, GpuTextureView view, GpuSampler sampler) {
		//? if >=26.3 {
		pass.setUniform(name, view, sampler);
		//?} else {
		/*pass.bindTexture(name, view, sampler);
		*///?}
	}

	/** Draws the full-screen triangle of a {@link #screenQuad} pipeline. */
	static void drawScreenQuad(RenderPass pass) {
		//? if >=26.2 {
		pass.draw(3, 1, 0, 0);
		//?} else {
		/*pass.draw(0, 3);
		*///?}
	}
}
