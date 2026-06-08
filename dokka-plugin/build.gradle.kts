plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly("org.jetbrains.dokka:dokka-core:2.2.0")
    implementation("org.jetbrains.dokka:dokka-base:2.2.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(25)
}