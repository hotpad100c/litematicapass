plugins {
    id("net.fabricmc.fabric-loom")
    id("maven-publish")
}

version = "${property("mod.version")}+${stonecutter.current.version}"
group = property("mod.group") as String
base.archivesName = property("mod.id") as String

val requiredJava = JavaVersion.VERSION_25

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
    implementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${prop("deps.fabric_api")}")

    implementation("maven.modrinth:${prop("deps.litematica")}")
    implementation("maven.modrinth:${prop("deps.malilib")}")
    implementation("maven.modrinth:${prop("deps.iris")}")
    implementation("maven.modrinth:${prop("deps.sodium")}")

    // Iris nests these libraries in its jar, but the development runtime does not load the nested jars of
    // dependencies; without them Iris fails as soon as it reads a shader pack.
    for (library in listOf(
        "org.anarres:jcpp:1.4.14",
        "org.antlr:antlr4-runtime:4.13.1",
        "io.github.douira:glsl-transformer:${prop("deps.glsl_transformer")}"
    )) {
        localRuntime(library) { isTransitive = false }
    }
}

// Development-only mod that checks every mixin applies; see src/audit and `runAuditClient`.
val audit: SourceSet = sourceSets.create("audit") {
    compileClasspath += sourceSets.main.get().compileClasspath + sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().runtimeClasspath + sourceSets.main.get().output
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json") // Useful for interface injection
    mods {
        register(property("mod.id") as String) { sourceSet(sourceSets.main.get()) }
        register("litematicapass_audit") { sourceSet(audit) }
    }
    runs {
        register("auditClient") {
            client()
            source(audit)
            // Fabric reports startup errors in a dialog unless headless; the audit must exit instead.
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

    // Builds the version into a shared folder in `build/libs/${mod version}/`
    register<Copy>("buildAndCollect") {
        group = "build"
        from(jar.map { it.archiveFile })
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
