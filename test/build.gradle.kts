import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    java
    id("com.gradleup.shadow")
    id("net.minecrell.plugin-yml.bukkit")
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("io.freefair.lombok") version "9.5.0"
}

version = "TEST"

repositories {
    mavenCentral()
    maven("https://repo.xenondevs.xyz/releases")
}

val minecraftVersion = property("minecraft.version").toString()

dependencies {
    compileOnly("io.papermc.paper:paper-api:$minecraftVersion.build.+")
    compileOnly(project(":rebar"))
    implementation("org.assertj:assertj-core:3.27.2")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

bukkit {
    name = "RebarTest"
    main = "io.github.pylonmc.rebar.test.RebarTest"
    version = project.version.toString()
    apiVersion = minecraftVersion
    depend = listOf("Rebar")
    load = BukkitPluginDescription.PluginLoadOrder.STARTUP
}

tasks.runServer {
    dependsOn(project(":rebar").tasks.shadowJar)
    val runFolder = project.projectDir.resolve("run")
    val testsFailedFile = runFolder.resolve("tests-failed")
    doFirst {
        runFolder.deleteRecursively()
        runFolder.mkdirs()
        runFolder.resolve("eula.txt").writeText("eula=true")
        testsFailedFile.delete()

        val pluginFolder = runFolder.resolve("plugins")
        pluginFolder.mkdirs()
        val archive = project(":rebar").tasks.shadowJar.map { it.archiveFile }.get().get().asFile
        archive.copyTo(pluginFolder.resolve(archive.name), overwrite = true)
    }
    maxHeapSize = "2G"
    minecraftVersion(minecraftVersion)
    doLast {
        runFolder.resolve("gametests").deleteRecursively()
        if (testsFailedFile.exists()) {
            throw GradleException("Tests failed")
        }
    }
}
