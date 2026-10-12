plugins {
    id("dev.kikugie.stonecutter")
    id("net.fabricmc.fabric-loom-remap") version "1.18-SNAPSHOT" apply false
}

stonecutter active "1.21.1"

// See https://stonecutter.kikugie.dev/wiki/config/params
// Versions whose newest Iris is older than 1.7, which still lived in net.coderbot.iris.
val legacyIris = setOf("1.20.2")

stonecutter parameters {
    replacements {
        string(current.version in legacyIris) {
            replace("net.irisshaders.iris.pipeline.IrisRenderingPipeline", "net.coderbot.iris.pipeline.newshader.NewWorldRenderingPipeline")
            replace("net.irisshaders.iris.vertices.ImmediateState", "net.coderbot.iris.vertices.ImmediateState")
            replace("import net.irisshaders.iris.Iris;", "import net.coderbot.iris.Iris;")
        }
    }
}
