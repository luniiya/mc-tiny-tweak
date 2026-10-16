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

loom {
    runs {
        // Multiplayer testing: start runServer, then runClient and runClient2. Client 2 joins localhost by itself.
        named("server") {
            runDir("run-server")
        }
        register("client2") {
            client()
            configName = "Minecraft Client 2"
            runDir("run-client2")
            programArgs("--username", "Tester2", "--quickPlayMultiplayer", "localhost")
        }
    }
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

tasks.test {
    useJUnitPlatform()
    // Tests run the preLaunch entrypoint, which writes options.txt into the game directory.
    val testGameDir = layout.buildDirectory.dir("test-game").get().asFile
    workingDir = testGameDir
    doFirst { testGameDir.mkdirs() }
    systemProperty("expected.minecraft.version", stonecutter.current.version)
    systemProperty("expected.mod.version", project.version.toString())
    testLogging {
        events("passed", "skipped", "failed")
    }
}
