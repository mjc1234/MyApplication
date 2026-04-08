plugins {
    alias(libs.plugins.example.android.library)
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

    // Testing
    testImplementation(libs.room.testing)
}
