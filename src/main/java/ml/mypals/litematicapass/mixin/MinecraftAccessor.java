package ml.mypals.litematicapass.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 1.21.6 之前：MinecraftMixin 在重定向期间改写 getMainRenderTarget()，我们自己需要真正的主目标时读这个字段。
 * 只在这些版本上应用（见 LitematicaPassMixinPlugin）。
 */
@Mixin(Minecraft.class)
public interface MinecraftAccessor {
	@Accessor("mainRenderTarget")
	RenderTarget litematicapass$mainRenderTarget();
}
