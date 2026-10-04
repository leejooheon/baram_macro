import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.material.icons.core)

    // Include the Test API
    testImplementation(libs.compose.ui.test.junit4)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.jnativehook)
    implementation(libs.jna)

//    implementation(files("libs/tess4j-5.10.0.jar"))
    implementation(libs.tess4j) {
        exclude(group = "net.sourceforge.tess4j", module = "tess4j")
    }

    implementation(libs.bundles.ktor)
}

compose.desktop {
    application {
        // 다른 앱 실행: gradlew run -PmainClass=JusulsaKt (OCR 모니터는 OcrMonitorKt)
        mainClass = (findProperty("mainClass") as String?) ?: "CommanderKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageName = "baram-macro"
            packageVersion = "1.0.0"
        }
    }
}
