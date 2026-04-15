plugins {
    alias(libs.plugins.example.android.compose.library)
    alias(libs.plugins.example.android.lint)
}

android {
    namespace = "com.example.navigation"
}

dependencies {
    implementation(libs.androidx.compose.material3.adaptive.navigation3)
}