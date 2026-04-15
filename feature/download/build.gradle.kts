plugins {
    alias(libs.plugins.example.android.compose.library)
    alias(libs.plugins.example.android.hilt)
    alias(libs.plugins.example.android.lint)
}

android {
    namespace = "com.mjc.feature.download"
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.workmanager)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.compose.material3.adaptive.navigation3)

    implementation(project(":core:download"))
    implementation(project(":core:navigation"))
}
