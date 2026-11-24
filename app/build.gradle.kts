import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
}

android {
  namespace = "cl.miproyecto.minegocio"
  compileSdk = 37
  defaultConfig {
    applicationId = "cl.miproyecto.minegocio"
    minSdk = 24
    targetSdk = 37
    versionCode = 1
    versionName = "1.0"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  // FIRMA DEL APK RELEASE
  // Los datos de la llave se leen de keystore.properties (en la raíz del proyecto).
  // Ese archivo y la llave (.jks) NO se suben a git: tienen contraseñas.
  // Si el archivo no existe, el release se arma sin firmar.
  val archivoFirma = rootProject.file("keystore.properties")
  if (archivoFirma.exists()) {
    val firma = Properties().apply { archivoFirma.inputStream().use { load(it) } }
    signingConfigs {
      create("release") {
        storeFile = rootProject.file(firma.getProperty("storeFile"))
        storePassword = firma.getProperty("storePassword")
        keyAlias = firma.getProperty("keyAlias")
        keyPassword = firma.getProperty("keyPassword")
      }
    }
  }

  buildTypes {
    release {
      if (archivoFirma.exists()) signingConfig = signingConfigs.getByName("release")
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
    aidl = false
    buildConfig = false
    shaders = false
  }

  // Tests locales con Robolectric: simula Android en el computador (no necesita emulador).
  testOptions {
    unitTests.isIncludeAndroidResources = true
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

kotlin {
  jvmToolchain(17)
}

ksp{
  arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
  // El BOM debe ir en ambas configuraciones; el inspector lo marca como duplicado por error
  @Suppress("AvoidDuplicateDependencies")
  implementation(platform(libs.androidx.compose.bom))
  @Suppress("AvoidDuplicateDependencies")
  androidTestImplementation(platform(libs.androidx.compose.bom))

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: JUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  // Base de datos en memoria y migraciones en tests locales
  testImplementation(libs.robolectric)
  testImplementation(libs.androidx.test.core)
  testImplementation(libs.androidx.test.ext.junit)
  testImplementation(libs.androidx.room.testing)

  // Instrumented tests: JUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Room (Base de datos SQLite)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)

  // Cámara (CameraX) y lector de códigos de barras (ML Kit, funciona sin internet)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.mlkit.barcode.scanning)

}
