plugins {
    id("net.fabricmc.fabric-loom-remap")
    id("maven-publish")
}

version = "${property("mod.version")}+${stonecutter.current.version}"
group = property("mod.group") as String
base.archivesName = property("mod.id") as String

val requiredJava = JavaVersion.VERSION_21

/**
 * A per-version property. `-Ptry.node=<this node> -Ptry.<name>=<value>` overrides it for this node only, to build
 * this node's code against another Minecraft version (and its dependencies).
 */
fun prop(name: String): String =
    (if (findProperty("try.node") == stonecutter.current.version) findProperty("try.$name") else null)?.toString()
        ?: property(name).toString()
repositories {
    /**
     * Restricts dependency search of the given [groups] to the [maven URL][url],
     * improving the setup speed.
     */
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
}

dependencies {
    minecraft("com.mojang:minecraft:${prop("minecraft")}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${prop("deps.fabric_api")}")

    modImplementation("maven.modrinth:${prop("deps.litematica")}")
    modImplementation("maven.modrinth:${prop("deps.malilib")}")
    modImplementation("maven.modrinth:${prop("deps.iris")}")
    modImplementation("maven.modrinth:${prop("deps.sodium")}")

    // Iris nests these libraries in its jar, but the development runtime does not load the nested jars of
    // dependencies; without them Iris fails as soon as it reads a shader pack.
    for (library in listOf(
        "org.anarres:jcpp:1.4.14",
        "org.antlr:antlr4-runtime:4.13.1",
        "io.github.douira:glsl-transformer:${prop("deps.glsl_transformer")}"
    )) {
        localRuntime(library) { isTransitive = false }
    }

    // The mods as players install them (intermediary names), for `runProductionSmoke`.
    for (mod in listOf(
        "net.fabricmc.fabric-api:fabric-api:${prop("deps.fabric_api")}",
        "maven.modrinth:${prop("deps.litematica")}",
        "maven.modrinth:${prop("deps.malilib")}",
        "maven.modrinth:${prop("deps.iris")}",
        "maven.modrinth:${prop("deps.sodium")}"
    )) {
        "productionRuntimeMods"(mod) { isTransitive = false }
    }
}

// Development-only mod that checks every mixin applies; see src/audit and `runAuditClient`.
val audit: SourceSet = sourceSets.create("audit") {
    compileClasspath += sourceSets.main.get().compileClasspath + sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().runtimeClasspath + sourceSets.main.get().output
}

// Development-only mod that creates a fresh world and enters it; see src/smoke and `runSmokeClient`.
val smoke: SourceSet = sourceSets.create("smoke") {
    compileClasspath += sourceSets.main.get().compileClasspath + sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().runtimeClasspath + sourceSets.main.get().output
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json") // Useful for interface injection
    mods {
        register(property("mod.id") as String) { sourceSet(sourceSets.main.get()) }
        register("litematicapass_audit") { sourceSet(audit) }
        register("litematicapass_smoke") { sourceSet(smoke) }
    }
    runs {
        register("auditClient") {
            client()
            source(audit)
            // Fabric reports startup errors in a dialog unless headless; the audit must exit instead.
            vmArg("-Djava.awt.headless=true")
        }
        register("smokeClient") {
            client()
            source(smoke)
            // Fabric reports startup errors in a dialog unless headless (the game window itself still opens).
            vmArg("-Djava.awt.headless=true")
        }
    }
    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1") // Adds names to lambdas - useful for mixins
    }

    runConfigs.all {
        ideConfigGenerated(true)
        vmArgs("-Dmixin.debug.export=true") // Exports transformed classes for debugging
        runDir = "../../run" // Shares the run directory between versions
    }
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava
}

tasks {
    withType<JavaCompile>().configureEach {
        options.release = requiredJava.majorVersion.toInt()
    }

    processResources {
        val props = mapOf(
            "id" to project.property("mod.id"),
            "name" to project.property("mod.name"),
            "version" to project.version,
            "minecraft" to prop("mod.mc_dep"),
            "java" to requiredJava.majorVersion
        )
        inputs.properties(props)

        filesMatching("fabric.mod.json") { expand(props) }

        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        inputs.property("mixinJava", mixinJava)
        filesMatching("*.mixins.json") { expand("java" to mixinJava) }
    }

    jar {
        val projectName = project.property("mod.id")
        inputs.property("projectName", projectName)
        from(rootProject.file("LICENSE")) {
            rename { "${it}_$projectName" }
        }
    }

    // The audit reads the classes Mixin exports; drop the ones from earlier runs (possibly of another version).
    named("runAuditClient") {
        val exported = rootProject.file("run/.mixin.out")
        doFirst { exported.deleteRecursively() }
    }

    // The in-game check (src/smoke) as a remapped jar, and a run of the real game with intermediary names and the
    // remapped jars, as players run it. Some dependencies only work there (Iris 1.8.0 / 1.8.1 has a mixin that
    // cannot apply in the development environment).
    val smokeJar = register<Jar>("smokeJar") {
        from(smoke.output)
        archiveClassifier = "smoke-dev"
        destinationDirectory = layout.buildDirectory.dir("devlibs")
    }
    val remapSmokeJar = register<net.fabricmc.loom.task.RemapJarTask>("remapSmokeJar") {
        inputFile = smokeJar.flatMap { it.archiveFile }
        archiveClassifier = "smoke"
        destinationDirectory = layout.buildDirectory.dir("smoke")
        addNestedDependencies = false
    }
    register<net.fabricmc.loom.task.prod.ClientProductionRunTask>("runProductionSmoke") {
        group = "fabric"
        mods.setFrom(remapJar, remapSmokeJar, project.configurations.named("productionRuntimeMods"))
        runDir = rootProject.layout.projectDirectory.dir("run")
        jvmArgs.add("-Djava.awt.headless=true")
    }

    // Builds the version into a shared folder in `build/libs/${mod version}/`
    register<Copy>("buildAndCollect") {
        group = "build"
        from(remapJar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = property("mod.id") as String
            from(components["java"])
        }
    }
}
