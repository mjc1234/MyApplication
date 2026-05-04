plugins {
    alias(libs.plugins.example.android.compose.library)
    alias(libs.plugins.example.android.hilt)
    alias(libs.plugins.example.android.lint)
}

android {
    namespace = "com.mjc.feature.renderer"
}

dependencies {
    implementation(libs.filament)
}