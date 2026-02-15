import org.gradle.kotlin.dsl.implementation
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_11)
                }
            }
        }
    }

//    listOf(
//        iosArm64(),
//        iosSimulatorArm64()
//    ).forEach { iosTarget ->
//        iosTarget.binaries.framework {
//            baseName = "ComposeApp"
//            isStatic = true
//        }
//    }

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.okhttp.logging)
            implementation(libs.newpipe.extractor)
        }

        // Only if iOS is enabled
//        iosMain.dependencies {
//            implementation(libs.ktor.client.darwin)
//        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(libs.compose.ui.text)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.androidx.navigation3.ui)
            implementation(libs.androidx.navigation3.runtime)
            implementation(libs.androidx.lifecycle.viewmodel.navigation3)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.serialization.json)
            implementation(compose.materialIconsExtended)

            implementation(libs.coil.compose)
            implementation(libs.coil.core)
            implementation(libs.coil.network.ktor)

            implementation(libs.media3.exoplayer)
            implementation(libs.media3.session)
            implementation(libs.media3.ui.compose)
            implementation(libs.media3.ui.compose.material)
            implementation(libs.media3.ui)

            implementation(libs.datastore.core)
            implementation(libs.datastore.preferences.core)

            implementation(project.dependencies.platform(libs.koin.bom))

            // Room dependencies
            implementation(libs.androidx.room.runtime)
            implementation(libs.sqlite.bundled)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "org.example.project"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.example.project"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// Room configuration - MUST be outside the android block
room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    debugImplementation(compose.uiTooling)

    // KSP for Room - Add for each target you're using
    add("kspCommonMainMetadata", "androidx.room:room-compiler:2.8.4")
    add("kspAndroid", "androidx.room:room-compiler:2.8.4")

    // Only add these if you enable iOS targets
//    add("kspIosX64", libs.room.compiler)
//    add("kspIosArm64", libs.room.compiler)
//    add("kspIosSimulatorArm64", libs.room.compiler)
}


