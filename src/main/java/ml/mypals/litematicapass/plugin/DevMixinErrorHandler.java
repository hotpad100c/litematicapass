package ml.mypals.litematicapass.plugin;

import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinErrorHandler;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * 仅开发环境：Iris 1.8.x（Minecraft 1.21.3 及更早）的 Sodium 兼容 mixin MixinRenderSectionManagerShadow 以中间名描述符
 * （class_4184）指定目标且没有 refMap，在开发环境的映射下找不到目标，进入世界时直接崩溃。生产环境中它正常工作。
 * 这里只把这一个 mixin 的失败降级为警告；Iris 在开发环境中因此少了阴影渲染时的区块剔除，但不影响测试。
 */
public final class DevMixinErrorHandler implements IMixinErrorHandler {
	private static boolean isIrisSodiumShadow(IMixinInfo mixin) {
		return mixin != null && mixin.getName().endsWith(".MixinRenderSectionManagerShadow")
			&& mixin.getConfig().getName().equals("mixins.iris.compat.sodium.json");
	}

	@Override
	public ErrorAction onPrepareError(IMixinConfig config, Throwable th, IMixinInfo mixin, ErrorAction action) {
		return isIrisSodiumShadow(mixin) ? ErrorAction.WARN : action;
	}

	@Override
	public ErrorAction onApplyError(String targetClassName, Throwable th, IMixinInfo mixin, ErrorAction action) {
		return isIrisSodiumShadow(mixin) ? ErrorAction.WARN : action;
	}
}
