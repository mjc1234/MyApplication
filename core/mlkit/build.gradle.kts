plugins {
    alias(libs.plugins.example.android.library)
    alias(libs.plugins.example.android.hilt)
    alias(libs.plugins.example.android.lint)
}

android {
    namespace = "com.mjc.core.mlkit"
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
}
