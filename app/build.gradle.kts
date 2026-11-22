import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Versione come su iPhone: semver per le persone, build AAAAMMGG.HHMM (ora di Roma) nel file VERSIONE.
val versioneMarketing = "0.3.0"
val build: String = rootProject.file("VERSIONE").readText().trim()

// Google Play vuole un versionCode intero e sempre crescente: minuti trascorsi dal 1/1/2026 alla build.
val codiceVersione: Int = run {
    val momento = LocalDateTime.parse(build, DateTimeFormatter.ofPattern("yyyyMMdd.HHmm"))
    ChronoUnit.MINUTES.between(LocalDateTime.of(2026, 1, 1, 0, 0), momento).toInt()
}

// Chiave di caricamento per Google Play: keystore.properties resta fuori dal repository (vedi README).
val firma = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "blog.argosnewdreams.argo"
    compileSdk = 37

    defaultConfig {
        applicationId = "blog.argosnewdreams.argo"
        minSdk = 31          // Android 12: trascrizione ML Kit sul dispositivo (modalità Base)
        targetSdk = 36       // requisito Google Play 2026
        versionCode = codiceVersione
        versionName = versioneMarketing
        buildConfigField("String", "BUILD_ARGO", "\"$build\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Solo ARM a 64 bit: tutti i telefoni Android 12+ di fascia media e alta. Dimezza le librerie native.
        ndk { abiFilters += "arm64-v8a" }
    }

    signingConfigs {
        if (firma.isNotEmpty()) {
            create("play") {
                storeFile = rootProject.file(firma.getProperty("storeFile"))
                storePassword = firma.getProperty("storePassword")
                keyAlias = firma.getProperty("keyAlias")
                keyPassword = firma.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (firma.isNotEmpty()) signingConfig = signingConfigs.getByName("play")
        }
        debug {
            applicationIdSuffix = ".lab"
            versionNameSuffix = "-lab"
        }
        // «Argo Lab» da installare a mano sul telefono: ottimizzata come la release, firmata con la chiave di debug
        create("lab") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".lab"
            versionNameSuffix = "-lab"
            matchingFallbacks += "release"
        }
    }

    // Interfaccia in italiano (base), inglese, spagnolo e tedesco: Android 13+ mostra la scelta anche
    // nelle impostazioni di sistema dell'app (locale config generato da res/values-*)
    androidResources {
        generateLocaleConfig = true
        localeFilters += listOf("it", "en", "es", "de")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    val bom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(bom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-service:2.11.0")
    implementation("androidx.navigation:navigation-compose:2.10.2")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.13.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.11.0")

    // Fotocamera dentro l'app: la registrazione non si ferma mentre scatti
    val camerax = "1.6.2"
    implementation("androidx.camera:camera-core:$camerax")
    implementation("androidx.camera:camera-camera2:$camerax")
    implementation("androidx.camera:camera-lifecycle:$camerax")
    implementation("androidx.camera:camera-view:$camerax")

    // Tutto sul dispositivo: trascrizione (Gemini Nano / modello di sistema) e OCR con modello incluso nell'app
    implementation("com.google.mlkit:genai-speech-recognition:1.0.0-alpha2")
    implementation("com.google.mlkit:text-recognition:16.0.1")

    // Argo locale: sherpa-onnx (k2-fsa, Apache 2.0) per Parakeet TDT v3 + Silero VAD.
    // AAR ufficiale da huggingface.co/csukuangfj/sherpa-onnx-libs (android/aar), versione fissata nel repository.
    implementation(files("libs/sherpa-onnx-1.12.21.aar"))

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
}
