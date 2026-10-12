plugins {
    id("dev.kikugie.stonecutter")
    id("net.fabricmc.fabric-loom") version "1.18-SNAPSHOT" apply false
    id("net.fabricmc.fabric-loom-remap") version "1.18-SNAPSHOT" apply false
}

stonecutter active "26.3"

// See https://stonecutter.kikugie.dev/wiki/config/params
stonecutter parameters {
    swaps["mod_version"] = "\"${property("mod.version")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"

    replacements {
        // 26.3 moved the render API from Blaze3D into RenderPearl
        string(eval(current.version, "<26.3")) {
            replace("com.mojang.renderpearl.api.buffers.GpuBufferSlice", "com.mojang.blaze3d.buffers.GpuBufferSlice")
            replace("com.mojang.renderpearl.api.commands.RenderPassDescriptor", "com.mojang.blaze3d.systems.RenderPassDescriptor")
            replace("com.mojang.renderpearl.api.commands.RenderPass", "com.mojang.blaze3d.systems.RenderPass")
            replace("com.mojang.renderpearl.api.GpuFormat", "com.mojang.blaze3d.GpuFormat")
            replace("com.mojang.renderpearl.api.pipeline.BindGroupLayout", "com.mojang.blaze3d.pipeline.BindGroupLayout")
            replace("com.mojang.renderpearl.api.pipeline.BlendFunction", "com.mojang.blaze3d.pipeline.BlendFunction")
            replace("com.mojang.renderpearl.api.pipeline.ColorTargetState", "com.mojang.blaze3d.pipeline.ColorTargetState")
            replace("com.mojang.renderpearl.api.pipeline.DepthStencilState", "com.mojang.blaze3d.pipeline.DepthStencilState")
            replace("com.mojang.renderpearl.api.pipeline.PrimitiveTopology", "com.mojang.blaze3d.PrimitiveTopology")
            replace("com.mojang.renderpearl.api.pipeline.RenderPipeline", "com.mojang.blaze3d.pipeline.RenderPipeline")
            replace("com.mojang.renderpearl.api.pipeline.UniformType", "com.mojang.blaze3d.shaders.UniformType")
            replace("com.mojang.renderpearl.api.textures.FilterMode", "com.mojang.blaze3d.textures.FilterMode")
            replace("com.mojang.renderpearl.api.textures.GpuSampler", "com.mojang.blaze3d.textures.GpuSampler")
            replace("com.mojang.renderpearl.api.textures.GpuTextureView", "com.mojang.blaze3d.textures.GpuTextureView")
        }

        // 1.21.11 renamed ResourceLocation to Identifier and started shipping JSpecify
        string(eval(current.version, "<1.21.11")) {
            replace("Identifier", "ResourceLocation")
            replace("org.jspecify.annotations.Nullable", "org.jetbrains.annotations.Nullable")
        }

        // Fabric API for 26.1 renamed ClientCommandManager to ClientCommands
        string(eval(current.version, "<26.1")) {
            replace("ClientCommands", "ClientCommandManager")
        }
    }
}
