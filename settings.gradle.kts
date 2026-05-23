pluginManagement {
    val useLocalDokka = extra.get("useRebarDokka").toString().toBoolean()

    repositories {
        if (useLocalDokka) mavenLocal()
        gradlePluginPortal()
    }

    val dokkaVersion = if (useLocalDokka) "2.3.0-rebar-SNAPSHOT" else "2.2.0"
    plugins {
        id("org.jetbrains.dokka") version dokkaVersion
        id("org.jetbrains.dokka-javadoc") version dokkaVersion
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "rebar-root"

include("rebar")
include("test")
include("nms")
include("dokka-plugin")