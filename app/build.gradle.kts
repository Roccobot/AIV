plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.roccobot.aiv"
    // Compiled against 37 because the Compose alpha demands it; the platform
    // itself is a preview one, installed from the SDK preview channel. targetSdk
    // stays on the last stable API: compiling against a newer SDK and opting in
    // to its runtime behaviour are two separate decisions, and only the first is
    // forced on us here.
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.roccobot.aiv"
        // minSdk 28 e' una scelta, non un valore di comodo: da li' in su
        // esiste ImageDecoder, che decodifica HEIF e rispetta l'orientamento
        // EXIF senza codice nostro. Sotto, servirebbe una seconda strada per
        // ogni formato, cioe' il doppio del codice per telefoni del 2017.
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "0.10"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    debugImplementation(libs.androidx.ui.tooling)
}
