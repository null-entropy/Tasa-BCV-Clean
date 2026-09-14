import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Carga de la configuración de firma desde keystore.properties (no commitear ese archivo).
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.tasabcv.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.tasabcv.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 19
        versionName = "4.5.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            if (keystoreProperties.isNotEmpty()) {
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
                // La ruta del storeFile en keystore.properties es relativa a la
                // raíz del proyecto, no al módulo app.
                storeFile = rootProject.file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
            }
            // Firmar con v1 (JAR) + v2 para máxima compatibilidad:
            //   v1: requerido por algunos launchers y stores de terceros en Android 7.x.
            //        Propiedad deprecada en AGP 8.x pero funcional; se mantiene a propósito.
            //   v2: scheme moderno, más rápido de verificar (habilitado por defecto).
            @Suppress("DEPRECATION")
            isV1SigningEnabled = true
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Solo firma si el keystore está configurado; evita romper el build debug.
            if (keystoreProperties.isNotEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi"
        )
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // El widget usa un AppWidgetProvider estándar (sin reflexión problemática).

    // Lint: activo pero no aborta el build (advierte, no bloquea).
    // Desactivamos checks que no aplican a este proyecto:
    //   - UnusedResource/IconMissingDensityFolder: los drawables vectoriales cubren todas las densidades.
    //   - Typos: nombres de strings son correctos en es-VE.
    //   - Overdraw: Compose gestiona el overdraw internamente; los Surface anidados son intencionales.
    lint {
        abortOnError = false
        checkReleaseBuilds = true
        warningsAsErrors = false
        disable += "UnusedResources"
        disable += "IconMissingDensityFolder"
        disable += "Typos"
        disable += "Overdraw"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Compose BOM: versiones coordinadas para toda la familia de libs de Compose.
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    // Solo los íconos usados (Calculate, AttachMoney, etc. están en extended).
    // Core no incluye el set completo; extended es necesario para este subset.
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

// Compila release y copia el APK firmado a la carpeta Descargas del usuario,
// renombrado con la versión (ej: TasaBCV-Cleam-v4.4.2.apk).
tasks.register("copyReleaseToDownloads") {
    dependsOn("assembleRelease")
    doLast {
        val apk = layout.buildDirectory.file("outputs/apk/release/app-release.apk").get().asFile
        if (!apk.exists()) {
            throw GradleException("No se encontró el APK release: ${apk.absolutePath}")
        }
        val downloadsDir = File(System.getProperty("user.home"), "Downloads")
        if (!downloadsDir.exists()) {
            throw GradleException("No existe la carpeta Descargas: ${downloadsDir.absolutePath}")
        }
        val target = File(downloadsDir, "TasaBCV-Cleam-v${android.defaultConfig.versionName}.apk")
        apk.copyTo(target, overwrite = true)
        println("APK copiado a: ${target.absolutePath}")
    }
}
