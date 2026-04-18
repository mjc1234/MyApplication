plugins {
    alias(libs.plugins.example.android.compose.library)
    alias(libs.plugins.example.android.hilt)
    alias(libs.plugins.example.android.lint)
}

android {
    namespace = "com.mjc.feature.videoplayer"
}

dependencies {
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.dash)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.exoplayer.smoothstreaming)
    implementation(libs.media3.ui)
    implementation(libs.media3.ui.compose)
    implementation(libs.media3.session)
    implementation(libs.media3.datasource.okhttp)
    implementation(libs.androidx.compose.material3.adaptive.navigation3)

    implementation(project(":core:navigation"))
    implementation(project(":core:ui"))
    implementation(project(":core:mediaeffect"))
}
