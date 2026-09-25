plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// Version unique définie dans le fichier VERSION à la racine (MAJEUR.MINEUR.CORRECTIF)
val appVersion = rootProject.file("VERSION").readText().trim()
val (vMajor, vMinor, vPatch) = appVersion.split(".").map { it.toInt() }
val keystorePath: String? = System.getenv("KEYSTORE_PATH")

android {
    namespace = "com.meowcha.game"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.meowcha.game"
        minSdk = 24
        targetSdk = 37
        versionCode = vMajor * 10000 + vMinor * 100 + vPatch
        versionName = appVersion
        // Adresse où l'app va chercher les packs de contenu (index.json + zips)
        buildConfigField(
            "String", "CONTENT_URL",
            "\"" + (System.getenv("CONTENT_URL") ?: "https://github.com/nicoooo972/meowcha/releases/download/content/") + "\"",
        )
    }

    // Clé de signature fixe (fournie par les secrets CI) : indispensable pour que
    // les mises à jour s'installent par-dessus l'ancienne version.
    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Spike rendu 3D (glTF/.glb) pour la 2.0.0 — voir ui/Scene3D.kt
    implementation("io.github.sceneview:sceneview:4.39.0")
}
