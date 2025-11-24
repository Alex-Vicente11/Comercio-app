import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  jacoco
}

android {
  namespace = "com.oax.comercioapp"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.oax.comercioapp"
    minSdk = 29
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  kotlinOptions {
    jvmTarget = "11"
  }
  buildFeatures {
    viewBinding = true
  }

  testOptions {
    unitTests.isReturnDefaultValues = true
  }
}

jacoco {
  toolVersion = "0.8.12"
}

tasks.register<JacocoReport>("jacocoTestReport") {
  dependsOn("testDebugUnitTest")

  reports {
    xml.required.set(true)
    html.required.set(true)
  }

  val fileFilter = listOf(
    "**/R.class",
    "**/R$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    "**/*Test*.*",
    "android/**/*.*",
    "**/databinding/*",
    "**/androidx/*"
  )

  val debugTree = fileTree("${project.buildDir}/tmp/kotlin-classes/debug") {
    exclude(fileFilter)
  }

  val mainSrc = "${project.projectDir}/src/main/java"

  sourceDirectories.setFrom(files(mainSrc))
  classDirectories.setFrom(files(debugTree))
  executionData.setFrom(fileTree(project.buildDir) {
    include("jacoco/testDebugUnitTest.exec")
  })
}

dependencies {

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.appcompat)
  implementation(libs.material)
  implementation(libs.androidx.constraintlayout)
  implementation(libs.androidx.lifecycle.livedata.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.ktx)
  implementation(libs.androidx.navigation.fragment.ktx)
  implementation(libs.androidx.navigation.ui.ktx)

  //Material Design
  implementation("com.google.android.material:material:1.12.0")

  // Retrofit for REST API calls
  implementation("com.squareup.retrofit2:retrofit:2.9.0")
  implementation("com.squareup.retrofit2:converter-gson:2.9.0")

  // OkHttp para logging e interceptors
  implementation("com.squareup.okhttp3:okhttp:4.12.0")
  implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

  // Coroutines for async operations
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
  implementation(libs.androidx.legacy.support.v4)
  implementation(libs.androidx.fragment.ktx)
  implementation(libs.play.services.cast.framework)

  // RecyclerView
  implementation("androidx.recyclerview:recyclerview:1.3.2")

  // DataStore (para guardar datos de forma segura)
  implementation("androidx.datastore:datastore-preferences:1.0.0")

  // Opcional: Encrypted SharedPreferences
  implementation("androidx.security:security-crypto:1.1.0-alpha06")
  // ============================================
  // TESTING DEPENDENCIES (JUnit 4)
  // ============================================

  // JUnit 4 - Framework de testing
  testImplementation("junit:junit:4.13.2")

  // AndroidX Testing
  testImplementation("androidx.arch.core:core-testing:2.2.0")
  testImplementation("androidx.test:core:1.5.0")
  testImplementation("androidx.test.ext:junit:1.1.5")

  // Kotlin Test
  testImplementation(kotlin("test"))

  // Coroutines Testing
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")

  // MockK - Mocking para Kotlin
  testImplementation("io.mockk:mockk:1.13.9")
  testImplementation("io.mockk:mockk-android:1.13.9")

  // Android Instrumentation Tests
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
}

// CONFIGURACIÓN DE TESTS (JUnit 4)
tasks.withType<Test> {
  testLogging {
    events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    showStandardStreams = true
    showExceptions = true
    showCauses = true
    showStackTraces = true
  }
}