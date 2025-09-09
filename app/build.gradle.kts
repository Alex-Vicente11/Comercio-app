plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
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
  implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
  
  // Coroutines for async operations
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
  implementation(libs.androidx.legacy.support.v4)
  implementation(libs.androidx.fragment.ktx)
    implementation(libs.play.services.cast.framework)

    testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)

  // Testing dependencies
  testImplementation("junit:junit:4.13.2")
  testImplementation("androidx.arch.core:core-testing:2.2.0")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
  testImplementation("io.mockk:mockk:1.13.9")

}