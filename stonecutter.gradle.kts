plugins {
    id("dev.kikugie.stonecutter")
    id("net.fabricmc.fabric-loom-remap") version "1.18-SNAPSHOT" apply false
}

stonecutter active "1.21.1"

// Versions whose newest Iris is older than 1.7, which still lived in net.coderbot.iris.
val legacyIris = setOf("1.20.2", "1.20", "1.19.3", "1.18.2", "1.18.1", "1.17.1")
// Versions with fixed-function rendering, where Iris's pipeline is DeferredWorldRenderingPipeline.
val fixedFunctionIris = setOf("1.16.5")

// See https://stonecutter.kikugie.dev/wiki/config/params
stonecutter parameters {
    replacements {
        string(current.version in legacyIris || current.version in fixedFunctionIris) {
            replace("net.irisshaders.iris.vertices.ImmediateState", "net.coderbot.iris.vertices.ImmediateState")
            replace("import net.irisshaders.iris.Iris;", "import net.coderbot.iris.Iris;")
        }
        string(current.version in legacyIris) {
            replace("net.irisshaders.iris.pipeline.IrisRenderingPipeline", "net.coderbot.iris.pipeline.newshader.NewWorldRenderingPipeline")
        }
        string(current.version in fixedFunctionIris) {
            replace("net.irisshaders.iris.pipeline.IrisRenderingPipeline", "net.coderbot.iris.pipeline.DeferredWorldRenderingPipeline")
        }
    }
}
