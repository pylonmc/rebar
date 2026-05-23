plugins {
    kotlin("jvm")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
}

group = "io.github.pylonmc"

repositories {
    mavenCentral()
    maven("https://repo.xenondevs.xyz/releases")
}

val minecraftVersion = property("minecraft.version").toString()

dependencies {
    paperweight.paperDevBundle("$minecraftVersion.build.+")
    compileOnly(project(":rebar"))
}

kotlin {
    jvmToolchain(25)
}