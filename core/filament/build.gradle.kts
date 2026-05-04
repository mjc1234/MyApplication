plugins {
    alias(libs.plugins.example.android.library)
    alias(libs.plugins.example.android.hilt)
    alias(libs.plugins.example.android.lint)
}

android {
    namespace = "com.mjc.core.filament"
}

dependencies {
    implementation(libs.filament)
}