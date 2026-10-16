# LitematicaPass 1.12.2（Ornithe + OptiFine）

这个分支（`1.12`）只支持 Minecraft 1.12.2 的 Ornithe 环境，不再使用 stonecutter。代码以 1.16.5 的实现（`1.21.1-` 分支，`versions/1.16.5`）为基准移植。在 1.12 上，光影来自 OptiFine，而不是 Iris。

## 环境

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| Minecraft | 1.12.2 | |
| Ornithe | gen1 中间名（calamus-intermediary 1.12.2） | Litematica / MaLiLib 的 Ornithe 版本用 gen1 构建；gen2 的中间名不同，不能混用 |
| 构建 | Gradle 8.5，fabric-loom 1.4.5，ploceus 1.4.2，MCP stable 39 名称 | 与 masa 构建 Litematica Ornithe 版的方式相同（需要 JDK 17–21 运行 Gradle） |
| Fabric Loader | ≥ 0.15（构建用 0.15.3） | |
| Litematica | litematica-ornithe-1.12.2-0.40.1 | Modrinth 版本 `iqj6EsbG` |
| MaLiLib | malilib-ornithe-1.12.2-0.60.2 | Modrinth 版本 `c5hvGuGn` |
| OSL | OSL 0.11.3 的全部模块：branding 0.3.2、config 0.4.2、core 0.5.0、entrypoints 0.4.2、keybinds 0.1.2、lifecycle-events 0.5.2、networking 0.6.4、resource-loader 0.2.8 | Litematica / MaLiLib 需要 entrypoints 与 resource-loader，Mod Menu 需要 keybinds；玩家也要安装。branding 用 0.3.2：0.2.3 没有 1.12.2 版本，0.3.0 的入口类不存在 |
| OptiFine | OptiFine 1.12.2 HD U G5 + Legacy OptiFabric 2.3.6+b1.7.3-1.14.4 | 可选；没有光影包时本模组什么都不做。OptiFabric 2.2.0 不能配 Fabric Loader 0.20（它调用 Loader 内部 tiny-remapper 的 `TinyRemapper.newRemapper()`，报 `NoSuchMethodError`）；2.3.6 自带 tiny-remapper |
| Java | 8 | Prism 默认可能挑 Java 25，要在实例设置里指定 Java 8 |

构建：`./gradlew build`（产物在 `build/libs/`）。开发客户端：`./gradlew runClient`。同一时间只运行一个客户端：它们共用 `run/`，Fabric Loader 在 `run/.fabric/tmp` 重映射 `run/mods` 里的模组，两个实例同时启动会报 `ClosedFileSystemException`。

**OptiFine 在开发环境中不能完整运行。** 把 Legacy OptiFabric 和 OptiFine 放进 `run/mods` 后，OptiFabric 能找到原版 jar（`build.gradle` 传了 `-Doptifabric.mc-jar`）并加载 OptiFine，但它在开发环境里重映射 OptiFine 补丁过的类时，不会重命名继承来的方法（例如 `IntegratedServer` 的 `tick` 仍叫 `C`），于是 Litematica 的 `MixinIntegratedServer` 找不到目标而崩溃。这是 OptiFabric 的开发环境问题（它的 README 也说不支持开发环境），正式实例中不会出现。要测光影，请用真实的 Ornithe 1.12.2 实例。

## 在 OptiFine 光影下挂接的位置

OptiFine 的 `EntityRenderer.renderWorldPass`（由 OptiFine 打补丁）在有光影包时的顺序如下（已用 OptiFine 1.12.2 HD U G5 的字节码核对）：

1. `Shaders.beginRenderPass`：绑定 OptiFine 自己的帧缓冲 `dfb`（G-buffers，按 OptiFine 的渲染分辨率）。
2. 原版固体 / 镂空地形 → **Litematica 的 `piecewiseRenderSolid` / `CutoutMipped` / `Cutout`**（Litematica 的 `MixinEntityRenderer`，注入在每次 `renderBlockLayer` 之后）。
3. 原版实体之前 → **Litematica 的 `piecewiseRenderEntities`**。
4. 粒子、天气。
5. `ShadersRender.renderHand0`：OptiFine 画不透明的第一人称手部。
6. `Shaders.preWater`：OptiFine 把深度复制成 `depthtex1`。
7. 半透明层（水）→ **Litematica 的 `piecewiseRenderTranslucent`**（含 schematic 的网格叠加层）。
8. malilib 的 world-last 事件（第一次读取 `renderHand` 字段处）→ **Litematica 的 `RenderHandler.onPostWorldRender`**（选区框、校验器；关闭 Better Render Order 时整个 schematic 也在这里画）。
9. `ShadersRender.renderHand1`：半透明的手持物品。
10. `Shaders.renderCompositeFinal`：composite 与 final 通道，把着色结果写进 Minecraft 的帧缓冲。
11. 清深度，`ShadersRender.renderFPOverlay`，`Shaders.endRender`。

Litematica 的所有绘制都在第 10 步之前，所以核心思路不变：

| 做什么 | 挂在哪里 | 怎么做 |
| --- | --- | --- |
| 重定向 Litematica 的绘制 | `LitematicaRenderer.piecewiseRender*`、`RenderHandler.onPostWorldRender` 的 HEAD / RETURN | 绑定我们自己的 FBO（颜色 + 深度纹理，大小跟随 `dfb` 的深度纹理）。开始前先让 OptiFine 切到 `ProgramNone`（`Shaders.useProgram`，在 `dfb` 仍绑定时调用），重定向期间取消 OptiFine 的 `useProgram` / `setDrawBuffers`，Litematica 就用固定管线画进我们的目标；结束时绑回原来的帧缓冲，把 OptiFine 原来的程序还给它 |
| 推迟固体、镂空和实体 | `LitematicaRenderer.piecewiseRenderSolid` / `CutoutMipped` / `Cutout` / `Entities` 的 HEAD（取消并记下），在 `Shaders.preWater` HEAD 重放 | Litematica 在原版固体层之后就画 schematic 的固体层，那时原版的镂空方块（草方块、树叶）和实体还没画，深度种子里没有它们，它们就挡不住 schematic。我们画进独立目标，与原版顺序无关，所以有光影包时推迟到不透明场景画完再画（`DeferredSchematic`，按原版方块层的状态设置 alpha 测试、mipmap 等） |
| 初始化 schematic 深度（让世界遮挡 schematic） | 每帧第一次重定向时（即上面的重放） | 读取当前绑定帧缓冲（`dfb`）的深度附件纹理，`depth_seed` 通道写入我们的深度（手部位置写最近深度） |
| 手部之前的深度（≈ depthtex2） | `ShadersRender.renderHand0` HEAD | `glCopyTexSubImage2D` 从仍绑定的 `dfb` 复制深度 |
| 半透明之前的深度（≈ depthtex1） | `Shaders.preWater` HEAD | 同上（OptiFine 自己在这里做同样的复制） |
| 最终深度（≈ depthtex0） | `Shaders.renderCompositeFinal` HEAD | 同上；1.12 的 OptiFine 不和 Minecraft 的帧缓冲共享深度，所以要自己存一份 |
| 按深度合并 | `Shaders.renderCompositeFinal` RETURN | `schematic_composite` 通道写到 Minecraft 的帧缓冲上（预乘 alpha）；兜底：`renderWorldPass` RETURN |
| 半透明遮挡变暗 50% | 合并通道 | 与 1.16.5 相同：最终深度比半透明之前的深度更近处就是半透明表面；schematic 在其后方时颜色 × 0.5 |
| 手部遮挡 | 种子 + 合并通道 | 与 1.16.5 相同：手部前后深度不同处丢弃 schematic |

OptiFine 不是编译依赖：mixin 用字符串指定 `net.optifine.shaders.Shaders` / `ShadersRender`（`@Pseudo`、`remap = false`、`require = 0`），其余通过反射（`OptiFineCompat`）读取公开的静态字段 `shaderPackLoaded`、`isShadowPass`、`activeProgram`、`ProgramNone` 和方法 `useProgram(Program)`。

这些 mixin 在单独的配置 `litematicapass.optifine.mixins.json` 里，由 `mm:early_risers` 入口（`OptiFineMixins`）在装了 `optifabric` 时注册。原因：OptiFabric 在它自己的 early riser 里才把 OptiFine 加到类路径上，晚于 fabric.mod.json 里列出的 mixin 配置的准备阶段；Mixin 在那时找不到 `@Pseudo` 目标就会直接丢掉它（日志里的 "Skipping virtual target"）。之后注册的配置会在 OptiFine 已在类路径上时才准备（OptiFabric 注册它自己的 OptiFine mixin 也是这样做的）。

与新版本的不同之处：

- 1.12 的 `Framebuffer` 深度是渲染缓冲，不能采样，所以 `Gpu` 自己管理纹理和 FBO，不用 Minecraft 的 `Framebuffer`。
- OptiFine 的渲染分辨率（Render Quality）可以和窗口不同：各个纹理大小不一，着色器按比例取对应像素（`at()`）。
- 1.12 的实体立即绘制（没有 buffer source），实体和地形一样直接用 begin/end 包住，不需要 `SchematicEntities`。
- 1.12 运行在兼容（compatibility）上下文里：全屏三角形用一个顶点缓冲（属性 0），绘制时关闭 alpha test、多边形偏移等固定管线状态，结束后全部还原。
- 调试命令 `/litematicapass debug [view|status]`：1.12 没有客户端命令 API（OSL 也没有），改为拦截 `EntityPlayerSP.sendChatMessage`。`status` 列出各挂接点触发、重定向、合并的次数和全屏通道后的 GL 错误（自上次 status 起）。
- LWJGL 2 把 `CharSequence` 形式的着色器源码按每个字符的低字节转成字节，着色器里的中文注释会变成乱码甚至 NUL，驱动报 "unexpected $end"、所有全屏通道都编译失败。现在把非 ASCII 字符换成空格，以字节传入。

## Litematica 在 OptiFine 光影下的问题（本模组一并修正）

这三处与重定向无关，不装本模组时同样存在，但不修就不能在光影下使用 Litematica：

- **切换光影包后崩溃**（"Tesselating block model"，`BufferBuilder` 越界）：OptiFine 开关光影包时把 `DefaultVertexFormats.BLOCK` 在 28 字节和 56 字节之间切换，Litematica 的区块构建缓冲只在第一次创建时按当时的格式建立。`RenderGlobalSchematicMixin` 在格式变化后的 `loadRenderers` 里丢掉旧的 dispatcher，让它按新格式重建。
- **VBO 步长**：Litematica 的 `VboRenderListSchematic.setupArrayPointersBlocks` 写死了 28 字节的步长，光影下的区块是 56 字节一个顶点（前 28 字节布局相同）。`VboRenderListSchematicMixin` 把步长改成当前方块格式的大小。
- **缺失方块的覆盖面只剩半个三角形**：Litematica 为缺失方块画基于模型的覆盖面和轮廓时，按每顶点 7 个 int 读取烘焙模型的顶点位置，光影下是 14 个 int。`RenderUtilsMixin` 把顶点数据按 7 个 int 一个顶点重新排好再交给它。

## 做不到 / 有差异的部分

- **半透明的手持物品**：OptiFine 在半透明之后才画它（`renderHand1`），所以它不在"手部前后深度"的差里，schematic 不会在那里被丢弃，而是被当成"在半透明表面后方"变暗 50%。替代方案：在 `renderHand1` 的 HEAD 再捕获一次深度，与最终深度比较得到半透明手部的区域（未实现）。
- **非 piecewise 渲染**：关闭 Litematica 的 "Better Render Order" 时，整个 schematic 在 world-last 事件里画（半透明之后），没有推迟这一步；深度种子用的是半透明之前的深度，应该没有问题，但没有测过。
- **阴影通道**：OptiFine 的阴影通道不经过 `renderWorldPass`，Litematica 不在其中绘制，所以 schematic 不投影（与 Iris 版相同）。

## 已验证 / 未验证

已验证：

- 分支在 Ornithe 1.12.2（gen1 + MCP 名称）下 `./gradlew build` 通过，产物的 refmap 中间名与 Litematica 自己的 refmap 一致（例如 `EntityRenderer.renderWorldPass` = `C_0259655.m_0906956`）。
- 开发客户端（无 OptiFine，只带四个 OSL 模块时）能启动并进入世界：`EntityPlayerSPMixin`、`GlStateManagerMixin`、`EntityRendererMixin`、`litematica.RenderHandlerMixin`、`litematica.LitematicaRendererMixin` 都已应用，没有错误。没有光影包时不重定向，画面与不装本模组相同。
- OptiFine 1.12.2 HD U G5 中挂接点的存在、签名和调用顺序（对 OptiFine 补丁后的 `EntityRenderer` 与 `Shaders` / `ShadersRender` 用 javap 核对）。
- 真实的 Ornithe 1.12.2 实例（Prism，Ornithe 安装器 0.16.0 生成的 gen1 实例，Fabric Loader 0.20.0，Java 8，OSL 0.16.3，Legacy OptiFabric 2.3.6，OptiFine 1.12.2 HD U G5，RTX 4070 / NVIDIA 610.62）：光影包 Chocapic13 V8 Ultra。作者在游戏里确认（2026-10-16）：schematic 在光影下显示、颜色正确（`debug color` 起作用）；草方块挡住 schematic；缺失方块的蓝色覆盖面完整。`debug status` 显示三个挂接点每帧触发、重定向与合并正常，所有 mixin 都已应用。

未验证：

- 半透明变暗、手部遮挡（尤其半透明手持物品，见上文）、除 `color` 外的调试视图、切换光影包后不再崩溃（修正已装上，但没有专门再切换测试）、关闭 "Better Render Order" 时的渲染、显示列表（关闭 VBO）路径。
- OptiFine 的 `checkGLError` 在重定向期间是否报错（例如 OptiFine 在实体渲染里设置 uniform 时）。
- 其它 OptiFine 1.12.2 版本（G5 以外）、其它显卡驱动。

测试步骤：Ornithe 1.12.2 实例的 mods 文件夹放入本模组、Litematica 0.40.1、MaLiLib 0.60.2、上面的 OSL 模块、Legacy OptiFabric 2.3.6+b1.7.3-1.14.4（Fabric Loader 0.20 下不能用 2.2.0）和 OptiFine 1.12.2 HD U G5，用 Java 8 启动；选一个光影包，加载一个 schematic，用 `/litematicapass debug color`、`scene_depth`、`main_depth`、`translucent_depth` 逐个检查缓冲区，再 `/litematicapass debug off` 看合并结果（水后的 schematic 应变暗，手部应在 schematic 上方）。
