plugins {
    alias(libs.plugins.example.android.compose.library)
    alias(libs.plugins.example.android.hilt)
    alias(libs.plugins.example.android.lint)
}

android {
    namespace = "com.mjc.feature.camera"
}

dependencies {
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.camerax.video)
    implementation(libs.camerax.compose)
    implementation(libs.androidx.compose.material3.adaptive.navigation3)

    implementation(project(":core:navigation"))
    implementation(project(":core:mlkit"))
}
