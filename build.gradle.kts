import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.material.icons.core)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.jnativehook)
    implementation(libs.jna)
    implementation(libs.jna.platform)

    implementation(libs.bundles.ktor)

    testImplementation(kotlin("test"))
}

compose.desktop {
    application {
        // 다른 앱 실행: gradlew run -PmainClass=OcrMonitorKt (OCR 모니터)
        mainClass = (findProperty("mainClass") as String?) ?: "JusulsaKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageName = "baram-macro"
            packageVersion = "1.0.0"
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
