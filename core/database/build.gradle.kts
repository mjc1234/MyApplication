plugins {
    alias(libs.plugins.example.android.library)
    alias(libs.plugins.example.android.hilt)
    alias(libs.plugins.example.android.lint)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "com.mjc.core.database"
    room {
        schemaDirectory("$projectDir/schemas")
    }
}

dependencies {
    // Room
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Testing
    testImplementation(libs.room.testing)
}
