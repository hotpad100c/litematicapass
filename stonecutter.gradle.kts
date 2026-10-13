plugins {
    id("dev.kikugie.stonecutter")
    id("net.fabricmc.fabric-loom-remap") version "1.18-SNAPSHOT" apply false
}

stonecutter active "1.21.1"

// The versions share one run directory (runDir in the build script). When several versions' run tasks are run in
// one build, each waits for every version's asset download (which writes there) and they run one after another.
fun versionParts(version: String) = version.split('.').map { it.toIntOrNull() ?: 0 }
fun isOlder(a: String, b: String): Boolean {
    val x = versionParts(a)
    val y = versionParts(b)
    for (i in 0 until maxOf(x.size, y.size)) {
        val d = x.getOrElse(i) { 0 } - y.getOrElse(i) { 0 }
        if (d != 0) return d < 0
    }
    return false
}
subprojects {
    val node = this
    tasks.withType<net.fabricmc.loom.task.AbstractRunTask>().configureEach {
        rootProject.subprojects.filter { it != node }.forEach { other ->
            mustRunAfter(other.tasks.matching { it.name == "downloadAssets" })
            if (isOlder(other.name, node.name)) {
                mustRunAfter(other.tasks.withType<net.fabricmc.loom.task.AbstractRunTask>())
            }
        }
    }
}

// Versions whose newest Iris is older than 1.7, which still lived in net.coderbot.iris.
val legacyIris = setOf("1.20.2", "1.20", "1.19.3", "1.18.2", "1.18.1", "1.17.1")
// Versions with fixed-function rendering, where Iris's pipeline is DeferredWorldRenderingPipeline.
val fixedFunctionIris = setOf("1.16.5")

// See https://stonecutter.kikugie.dev/wiki/config/params
stonecutter parameters {
    replacements {
        string(current.version in legacyIris || current.version in fixedFunctionIris) {
            replace("net.irisshaders.iris.vertices.ImmediateState", "net.coderbot.iris.vertices.ImmediateState")
            replace("net.irisshaders.iris.pipeline.WorldRenderingPhase", "net.coderbot.iris.pipeline.WorldRenderingPhase")
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
