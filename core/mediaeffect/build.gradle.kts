plugins {
    alias(libs.plugins.example.android.library)
    alias(libs.plugins.example.android.hilt)
    alias(libs.plugins.example.android.lint)
}

android {
    namespace = "com.mjc.core.mediaeffect"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    api(libs.media3.effect)
}