import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.0.20"
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)

    // Include the Test API
    testImplementation(compose.desktop.uiTestJUnit4)

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("com.github.kwhat:jnativehook:2.2.2")

    implementation(files("libs/tess4j-5.10.0.jar"))
    implementation("net.sourceforge.tess4j:tess4j:5.10.0") {
        exclude(group = "net.sourceforge.tess4j", module = "tess4j")
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "KotlinJvmComposeDesktopApplication"
            packageVersion = "1.0.0"
        }
    }
}
