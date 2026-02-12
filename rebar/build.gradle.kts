import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    kotlin("jvm")
    `java-library`
    id("com.gradleup.shadow") version "9.0.0"
    id("de.eldoria.plugin-yml.paper") version "0.7.1"
    idea
    `maven-publish`
    signing
    id("org.jetbrains.dokka")
    id("org.jetbrains.dokka-javadoc")
}

repositories {
    mavenLocal()
    maven("https://repo.xenondevs.xyz/releases") {
        name = "InvUI"
    }
    maven("https://jitpack.io") {
        name = "JitPack"
    }
}

val minecraftVersion = property("minecraft.version").toString()

dependencies {
    fun paperLibraryApi(dependency: Any) {
        paperLibrary(dependency)
        compileOnlyApi(dependency)
    }

    runtimeOnly(project(":nms"))

    paperLibraryApi("org.jetbrains.kotlin:kotlin-stdlib:${kotlin.coreLibrariesVersion}")
    paperLibraryApi("org.jetbrains.kotlin:kotlin-reflect:${kotlin.coreLibrariesVersion}")
    paperLibraryApi("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    compileOnly("io.papermc.paper:paper-api:$minecraftVersion-R0.1-SNAPSHOT")

    paperLibraryApi("com.github.shynixn.mccoroutine:mccoroutine-bukkit-api:2.22.0")
    paperLibraryApi("com.github.shynixn.mccoroutine:mccoroutine-bukkit-core:2.22.0")
    paperLibraryApi("xyz.xenondevs.invui:invui:2.0.0-beta.1")
    paperLibraryApi("xyz.xenondevs.invui:invui-kotlin:2.0.0-beta.1")
    implementation("com.github.Tofaa2.EntityLib:spigot:f0318e298f")
    implementation("com.github.retrooper:packetevents-spigot:2.11.2")
    implementation("info.debatty:java-string-similarity:2.0.0")
    implementation("org.bstats:bstats-bukkit:2.2.1")
    paperLibrary("com.github.ben-manes.caffeine:caffeine:3.2.2")

    dokkaPlugin(project(":dokka-plugin"))

    testImplementation(kotlin("test"))
    testImplementation("com.willowtreeapps.assertk:assertk:0.28.1")
    testImplementation("net.kyori:adventure-api:4.20.0")
    testImplementation("net.kyori:adventure-text-minimessage:4.20.0")
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        javaParameters = true
        freeCompilerArgs = listOf("-Xjvm-default=all", "-Xwhen-guards")
    }
}

dokka {
    dokkaPublications.html {
        outputDirectory.set(layout.buildDirectory.dir("dokka/docs/kdoc"))
    }
    dokkaPublications.javadoc {
        outputDirectory.set(layout.buildDirectory.dir("dokka/docs/javadoc"))
    }
    dokkaSourceSets.configureEach {
        externalDocumentationLinks.register("Paper") {
            url("https://jd.papermc.io/paper/$minecraftVersion/")
            packageListUrl("https://jd.papermc.io/paper/$minecraftVersion/element-list")
        }
        externalDocumentationLinks.register("JOML") {
            url("https://javadoc.io/doc/org.joml/joml/latest/")
            packageListUrl("https://javadoc.io/doc/org.joml/joml/latest/element-list")
        }
        externalDocumentationLinks.register("Adventure") {
            url("https://javadoc.io/doc/net.kyori/adventure-api/latest/")
            packageListUrl("https://javadoc.io/doc/net.kyori/adventure-api/latest/element-list")
        }
        externalDocumentationLinks.register("InvUI") {
            url("https://invui.javadoc.xenondevs.xyz/")
            packageListUrl("https://invui.javadoc.xenondevs.xyz/element-list")
        }
        sourceLink {
            localDirectory.set(file("src/main/kotlin"))
            remoteUrl("https://github.com/pylonmc/rebar")
            remoteLineSuffix.set("#L")
        }
    }
    dokkaPublications.configureEach {
        suppressObviousFunctions = true
    }
}

tasks.dokkaGeneratePublicationJavadoc {
    // Fixes search lag by limiting the number of results shown
    // See https://github.com/Kotlin/dokka/issues/4284
    doLast {
        val searchJs = layout.buildDirectory.file("dokka/docs/javadoc/search.js").get().asFile
        val text = searchJs.readText()
        val codeToFix = "const result = [...modules, ...packages, ...types, ...members, ...tags]"
        if (codeToFix !in text) {
            throw IllegalStateException("Seggan you buffoon, you updated dokka without checking to see if the search fix still works")
        }
        val fixed = "const result = [" +
                "...modules.slice(0, 5), " +
                "...packages.slice(0, 5), " +
                "...types.slice(0, 40), " +
                "...members.slice(0, 40), " +
                "...tags.slice(0, 5)" +
                "]"
        searchJs.writeText(text.replace(codeToFix, fixed))
    }
}

val javadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
    dependsOn(tasks.dokkaGeneratePublicationHtml)
    archiveClassifier.set("javadoc")
    from(layout.buildDirectory.dir("dokka/docs/kdoc"))
}

tasks.shadowJar {
    mergeServiceFiles()

    exclude("kotlin/**")
    exclude("org/intellij/lang/annotations/**")
    exclude("org/jetbrains/annotations/**")

    relocate("com.github.retrooper.packetevents", "io.github.pylonmc.rebar.packetevents")
    relocate("me.tofaa.entitylib", "io.github.pylonmc.rebar.entitylib")
    relocate("org.bstats", "io.github.pylonmc.rebar.bstats")

    archiveBaseName = "rebar"
    archiveClassifier = null
}

paper {
    generateLibrariesJson = true

    name = "Rebar"
    loader = "io.github.pylonmc.rebar.RebarLoader"
    bootstrapper = "io.github.pylonmc.rebar.RebarBootstrapper"
    main = "io.github.pylonmc.rebar.Rebar"
    version = project.version.toString()
    authors = listOf("Rebar team")
    apiVersion = minecraftVersion
    load = BukkitPluginDescription.PluginLoadOrder.STARTUP
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "rebar"

            artifact(tasks.jar)
            artifact(tasks.kotlinSourcesJar)
            artifact(javadocJar)

            pom {
                name = artifactId
                description = "The core library for Rebar addons."
                url = "https://github.com/pylonmc/rebar"
                licenses {
                    license {
                        name = "GNU Lesser General Public License Version 3"
                        url = "https://www.gnu.org/licenses/lgpl-3.0.txt"
                    }
                }
                developers {
                    developer {
                        id = "PylonMC"
                        name = "PylonMC"
                        organizationUrl = "https://github.com/pylonmc"
                    }
                }
                scm {
                    connection = "scm:git:git://github.com/pylonmc/rebar.git"
                    developerConnection = "scm:git:ssh://github.com:pylonmc/rebar.git"
                    url = "https://github.com/pylonmc/rebar"
                }
                // Bypass maven-publish erroring when using `from(components["java"])`
                withXml {
                    val root = asNode()
                    val dependenciesNode = root.appendNode("dependencies")
                    val configs = listOf(configurations.compileOnlyApi, configurations.api)
                    configs.flatMap { it.get().dependencies }.forEach {
                        val dependencyNode = dependenciesNode.appendNode("dependency")
                        dependencyNode.appendNode("groupId", it.group)
                        dependencyNode.appendNode("artifactId", it.name)
                        dependencyNode.appendNode("version", it.version)
                        dependencyNode.appendNode("scope", "compile")
                    }
                }
            }
        }
    }
}

signing {
    useInMemoryPgpKeys(System.getenv("SIGNING_KEY"), System.getenv("SIGNING_PASSWORD"))

    sign(publishing.publications["maven"])
}

tasks.withType(Sign::class) {
    onlyIf {
        System.getenv("SIGNING_KEY") != null && System.getenv("SIGNING_PASSWORD") != null
    }
}