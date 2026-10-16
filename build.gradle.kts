plugins {
    id("dev.kikugie.stonecutter")
    id("net.fabricmc.fabric-loom-remap") version "1.14.10"
    kotlin("jvm") version "2.4.21"
}

group = "dev.luniiya"
version = "${property("mod_version")}+mc${stonecutter.current.version}"

base {
    archivesName.set("mc-tiny-tweak")
}

repositories {
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:${stonecutter.current.version}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")

    testImplementation("net.fabricmc:fabric-loader-junit:${property("loader_version")}")
}

kotlin {
    jvmToolchain(21)
}

java {
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}

tasks.processResources {
    val metadata = mapOf(
        "version" to project.version,
        "minecraft_version" to stonecutter.current.version,
        "loader_version" to project.property("loader_version"),
        "fabric_kotlin_version" to project.property("fabric_kotlin_version"),
    )
    inputs.properties(metadata)
    filesMatching("fabric.mod.json") {
        expand(metadata)
    }
}

// Each machine can point this at its Prism instance's game folder in ~/.gradle/gradle.properties.
val prismInstance = providers.gradleProperty("mctinytweak.prismInstance")

val copyPrismOptions by tasks.registering {
    group = "fabric"
    description = "Copies options.txt from the configured Prism instance so the dev client uses your settings."
    val source = prismInstance.map { File(it, "options.txt") }
    val target = layout.projectDirectory.file("run/options.txt").asFile
    outputs.upToDateWhen { false }
    doLast {
        val file = source.orNull
        when {
            file == null -> logger.lifecycle("mctinytweak.prismInstance is not set; keeping the dev client's own settings.")
            !file.isFile -> logger.warn("No options.txt in Prism instance at ${file.parent}; keeping the dev client's own settings.")
            else -> {
                target.parentFile.mkdirs()
                file.copyTo(target, overwrite = true)
                logger.lifecycle("Copied settings from $file")
            }
        }
    }
}

tasks.named("runClient") {
    dependsOn(copyPrismOptions)
}

tasks.test {
    useJUnitPlatform()
    systemProperty("expected.minecraft.version", stonecutter.current.version)
    systemProperty("expected.mod.version", project.version.toString())
    testLogging {
        events("passed", "skipped", "failed")
    }
}
