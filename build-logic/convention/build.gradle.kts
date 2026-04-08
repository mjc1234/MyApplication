import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl` // 必须开启，用于编写预编译脚本插件
}

group = "com.example.buildlogic" // 自定义你的插件组名

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.android.tools.common)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.hilt.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = libs.plugins.example.android.library.get().pluginId
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidComposeLibrary") {
            id = libs.plugins.example.android.compose.library.get().pluginId
            implementationClass = "AndroidComposeLibraryConventionPlugin"
        }
        register("androidHilt") {
            id = libs.plugins.example.android.hilt.get().pluginId
            implementationClass = "HiltConventionPlugin"
        }
    }
}