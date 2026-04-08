plugins {
    alias(libs.plugins.example.android.compose.library)
    alias(libs.plugins.example.android.hilt)
}

android {
    namespace = "com.mjc.feature.download"
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.workmanager)
    implementation(project(":core:download"))
    implementation(libs.hilt.navigation.compose)
}
